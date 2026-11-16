package com.fincore.server.service;

import com.fincore.server.dto.company.CompanyOnboardingRequest;
import com.fincore.server.dto.company.CompanyRequest;
import com.fincore.server.dto.company.InitialIdentifier;
import com.fincore.server.dto.discovery.DiscoveryContracts.Imported;
import com.fincore.server.dto.discovery.DiscoveryContracts.Match;
import com.fincore.server.dto.discovery.DiscoveryContracts.Results;
import com.fincore.server.dto.source.RawArtifactRequest;
import com.fincore.server.dto.source.SourceReferenceRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.CompanyServiceInter;
import com.fincore.server.models.company.CompanyIdentifier;
import com.fincore.server.repository.company.CompanyIdentifierRepository;
import com.fincore.server.repository.source.RawArtifactRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Discovers SEC directory identities and atomically imports explicitly selected companies. */
@Service
public class CompanyDiscoveryService {
  private final SecDirectoryClient directory;
  private final CompanyIdentifierRepository identifiers;
  private final CompanyDataServiceInter data;
  private final CompanyServiceInter companies;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;
  private final RawArtifactRepository artifacts;

  /**
   * Coordinates upstream retrieval outside transactions and database-serialized identity import.
   */
  public CompanyDiscoveryService(
      SecDirectoryClient directory,
      CompanyIdentifierRepository identifiers,
      CompanyDataServiceInter data,
      CompanyServiceInter companies,
      JdbcTemplate jdbc,
      TransactionTemplate transactions,
      RawArtifactRepository artifacts) {
    this.directory = directory;
    this.identifiers = identifiers;
    this.data = data;
    this.companies = companies;
    this.jdbc = jdbc;
    this.transactions = transactions;
    this.artifacts = artifacts;
  }

  /** Searches names, tickers or exact normalized CIKs without writing company records. */
  public Results search(String query, int page, int size) {
    if (query == null
        || query.isBlank()
        || query.length() > 100
        || page < 0
        || size < 1
        || size > 100) {
      throw new IllegalArgumentException("Invalid SEC discovery query or pagination");
    }
    String term = query.trim().toLowerCase(Locale.ROOT);
    String cik =
        term.matches("[0-9]{1,10}")
            ? String.format(Locale.ROOT, "%010d", Long.parseLong(term))
            : "";
    var source = directory.directory();
    var matches =
        source.entries().stream()
            .filter(
                e ->
                    e.name().toLowerCase(Locale.ROOT).contains(term)
                        || e.ticker().toLowerCase(Locale.ROOT).contains(term)
                        || e.cik().equals(cik))
            .sorted(
                Comparator.comparing(
                        (SecDirectoryClient.Entry e) -> !e.ticker().equalsIgnoreCase(term))
                    .thenComparing(SecDirectoryClient.Entry::name)
                    .thenComparing(SecDirectoryClient.Entry::ticker)
                    .thenComparing(SecDirectoryClient.Entry::cik))
            .toList();
    long offset = (long) page * size;
    var selected = matches.stream().skip(offset).limit(size).toList();
    Map<String, UUID> existing =
        existing(selected.stream().map(SecDirectoryClient.Entry::cik).toList());
    return new Results(
        selected.stream()
            .map(e -> new Match(e.cik(), e.ticker(), e.name(), existing.get(e.cik())))
            .toList(),
        page,
        size,
        matches.size(),
        offset + size < matches.size(),
        SecDirectoryClient.SOURCE_URL,
        source.retrievedAt());
  }

  /** Imports only identity and observed ticker aliases; never creates financial coverage. */
  public Imported importCompany(String cik) {
    if (cik == null || !cik.matches("(?!0000000000)[0-9]{10}")) {
      throw new IllegalArgumentException("CIK must contain ten digits");
    }
    var source = directory.directory();
    var rows = source.entries().stream().filter(e -> e.cik().equals(cik)).toList();
    if (rows.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "CIK not found in SEC ticker directory");
    }
    return transactions.execute(
        status -> {
          jdbc.execute(
              (ConnectionCallback<Void>)
                  connection -> {
                    try (var statement =
                        connection.prepareStatement("select pg_advisory_xact_lock(?)")) {
                      statement.setLong(1, 73000000000L + Long.parseLong(cik));
                      statement.execute();
                    }
                    return null;
                  });
          UUID prior = existing(List.of(cik)).get(cik);
          if (prior != null) {
            return new Imported(companies.get(prior), false, "identity_only");
          }
          // All directory imports share one cached artifact. Serialize its lookup/create as well.
          jdbc.execute(
              (ConnectionCallback<Void>)
                  connection -> {
                    try (var statement =
                        connection.prepareStatement("select pg_advisory_xact_lock(?)")) {
                      statement.setLong(1, 73000000000L);
                      statement.execute();
                    }
                    return null;
                  });
          UUID artifact =
              artifacts
                  .findBySha256AndStorageUri(source.sha256(), source.storageUri())
                  .map(com.fincore.server.models.source.RawArtifact::getId)
                  .orElseGet(
                      () ->
                          data.save(
                                  data.resource("raw-artifacts"),
                                  new RawArtifactRequest(
                                      Set.of(),
                                      source.storageUri(),
                                      source.sha256(),
                                      "response_bytes",
                                      "application/json",
                                      source.byteLength(),
                                      "json",
                                      source.retrievedAt(),
                                      source.sha256()))
                              .getId());
          UUID evidence =
              data.save(
                      data.resource("source-references"),
                      new SourceReferenceRequest(
                          Set.of(),
                          null,
                          artifact,
                          null,
                          SecDirectoryClient.SOURCE_URL,
                          "directory entries with cik_str=" + Long.parseLong(cik),
                          "api",
                          "sec-directory-v1",
                          source.retrievedAt(),
                          null,
                          "unknown",
                          null))
                  .getId();
          List<InitialIdentifier> initial = new ArrayList<>();
          initial.add(new InitialIdentifier(Set.of(evidence), "cik", "SEC", cik, null, null));
          rows.stream()
              .map(SecDirectoryClient.Entry::ticker)
              .distinct()
              .forEach(
                  ticker ->
                      initial.add(
                          new InitialIdentifier(
                              Set.of(evidence),
                              "other",
                              "SEC ticker directory:" + source.sha256(),
                              ticker,
                              null,
                              null)));
          var company =
              companies.create(
                  new CompanyOnboardingRequest(
                      new CompanyRequest(
                          Set.of(evidence), "sec-" + cik, rows.getFirst().name(), "issuer"),
                      initial));
          return new Imported(company, true, "identity_only");
        });
  }

  private Map<String, UUID> existing(List<String> ciks) {
    Map<String, UUID> matches = new HashMap<>();
    if (ciks.isEmpty()) {
      return matches;
    }
    for (CompanyIdentifier identifier : identifiers.findCiks(ciks)) {
      UUID prior = matches.putIfAbsent(identifier.getValue(), identifier.getCompanyId());
      if (prior != null && !prior.equals(identifier.getCompanyId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ambiguous stored CIK identity");
      }
    }
    return matches;
  }
}

package com.fincore.server.service;

import com.fincore.server.dto.disclosure.FilingRequest;
import com.fincore.server.dto.financial.FinancialConceptRequest;
import com.fincore.server.dto.financial.FinancialObservationRequest;
import com.fincore.server.dto.ingestion.IngestionContracts.Target;
import com.fincore.server.dto.ingestion.SecFinancialContracts;
import com.fincore.server.dto.ingestion.SecFinancialContracts.Completion;
import com.fincore.server.dto.ingestion.SecFinancialContracts.Fact;
import com.fincore.server.dto.source.IngestionAttemptRequest;
import com.fincore.server.dto.source.NormalizationRunRequest;
import com.fincore.server.dto.source.RawArtifactRequest;
import com.fincore.server.dto.source.SourceReferenceRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.Filing;
import com.fincore.server.models.source.ProviderEntityBinding;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Commits selected raw SEC facts and their evidence atomically, without financial selection. */
@Service
@Transactional
public class SecFinancialCommitService {
  private final IngestionJobService jobs;
  private final CompanyDataServiceInter data;
  private final JdbcTemplate jdbc;

  /** Uses the same database transaction as lease fencing, domain persistence and job completion. */
  public SecFinancialCommitService(
      IngestionJobService jobs, CompanyDataServiceInter data, JdbcTemplate jdbc) {
    this.jobs = jobs;
    this.data = data;
    this.jdbc = jdbc;
  }

  /** Replayed acknowledgement returns the original result without duplicating assertions. */
  public Map<String, Object> complete(UUID id, Completion input) {
    var job = jobs.lock(id, input.leaseToken());
    if ("succeeded".equals(job.get("status"))) {
      return job;
    }
    Target target = jobs.target((UUID) job.get("targetId"));
    ProviderEntityBinding binding = jobs.binding(target);
    if (!"sec_company_facts".equals(target.operation())
        || !target.adapterId().equals(input.adapterId())
        || !SecFinancialContracts.PARSER_ID.equals(input.adapterId())
        || !target.sourceId().equals(input.sourceId())
        || !SecFinancialContracts.SOURCE.equals(input.sourceId())
        || !binding.getSourceIdentifier().equals(input.cik())) {
      throw new IllegalArgumentException(
          "Financial completion identity or pinned contract mismatch");
    }
    if (input.requestedAt().isAfter(input.retrievedAt())
        || input.retrievedAt().isAfter(Instant.now().plusSeconds(30))) {
      throw new IllegalArgumentException("Invalid fetch timestamps");
    }
    SecFinancialIngestionService.validateSelection(target.selection());
    Set<String> selected = new HashSet<>(target.selection().concepts());
    Set<String> seen = new HashSet<>();
    Set<String> locators = new HashSet<>();
    for (Fact fact : input.records()) {
      validate(fact, target, locators);
      seen.add(fact.concept());
    }
    if (!seen.equals(selected)) {
      throw new IllegalArgumentException("Every selected concept must contain observations");
    }
    // Concepts and filing accession stubs are shared across refreshes. Protect their creation.
    jdbc.queryForList("SELECT pg_advisory_xact_lock(74000000001)");
    UUID artifact =
        save(
                "raw-artifacts",
                new RawArtifactRequest(
                    Set.of(),
                    input.artifact().storageUri(),
                    input.artifact().sha256(),
                    "response_bytes",
                    input.artifact().contentType(),
                    input.artifact().byteLength(),
                    "json",
                    input.artifact().storedAt(),
                    input.artifact().sha256()))
            .getId();
    UUID attempt =
        save(
                "ingestion-attempts",
                new IngestionAttemptRequest(
                    Set.of(),
                    binding.getProviderId(),
                    target.endpointId(),
                    target.bindingId(),
                    binding.getCompanyId(),
                    "company-facts-selected",
                    target.fetchUrl(),
                    input.requestedAt(),
                    input.retrievedAt(),
                    200,
                    "succeeded",
                    null,
                    null,
                    artifact,
                    Map.of("sourceId", target.sourceId(), "selection", target.selection()),
                    null,
                    true))
            .getId();
    Map<String, UUID> concepts = new HashMap<>();
    Map<String, UUID> filings = new HashMap<>();
    List<UUID> outputs = new ArrayList<>();
    for (Fact fact : input.records()) {
      UUID evidence =
          save(
                  "source-references",
                  new SourceReferenceRequest(
                      Set.of(),
                      attempt,
                      artifact,
                      null,
                      target.fetchUrl(),
                      fact.locator(),
                      "api",
                      SecFinancialContracts.PARSER_ID,
                      input.retrievedAt(),
                      fact.filedOn().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                      "date",
                      null))
              .getId();
      UUID concept = concepts.computeIfAbsent(fact.concept(), ignored -> concept(fact, evidence));
      UUID filing =
          filings.computeIfAbsent(
              fact.accession(), ignored -> filing(fact, target, binding, evidence));
      Filing storedFiling = (Filing) data.load("filings", filing);
      if (!fact.form().equals(storedFiling.getFormType())
          || !fact.filedOn().equals(storedFiling.getFilingDate())) {
        throw new IllegalArgumentException("Conflicting filing metadata for one accession");
      }
      UUID observation =
          save(
                  "financial-observations",
                  new FinancialObservationRequest(
                      Set.of(evidence),
                      binding.getCompanyId(),
                      concept,
                      filing,
                      null,
                      fact.periodStart() == null ? "instant" : "duration",
                      fact.periodStart(),
                      fact.periodEnd(),
                      fact.fiscalYear(),
                      fact.fiscalPeriod(),
                      fact.frame(),
                      "unspecified",
                      null,
                      List.of(),
                      "not_reported",
                      fact.value(),
                      fact.unit(),
                      0,
                      null,
                      fact.reportedValueRaw(),
                      fact.filedOn(),
                      null,
                      null,
                      input.cik(),
                      SecFinancialContracts.VERSION + ":" + id + ":" + hash(fact.locator())))
              .getId();
      outputs.add(observation);
    }
    UUID run =
        save(
                "normalization-runs",
                new NormalizationRunRequest(
                    Set.of(),
                    attempt,
                    SecFinancialContracts.PARSER_ID,
                    SecFinancialContracts.PARSER_ID,
                    SecFinancialContracts.PARSER_ID,
                    1,
                    SecFinancialContracts.VERSION,
                    input.retrievedAt(),
                    Instant.now(),
                    "succeeded",
                    outputs,
                    List.of()))
            .getId();
    jobs.succeedFinancial(id, run, outputs.size());
    return jobs.get(id);
  }

  private void validate(Fact fact, Target target, Set<String> locators) {
    String expected =
        "/facts/"
            + pointer(fact.taxonomy())
            + "/"
            + pointer(fact.concept())
            + "/units/"
            + pointer(fact.unit())
            + "/";
    if (!fact.taxonomy().equals(target.selection().taxonomy())
        || !target.selection().concepts().contains(fact.concept())
        || !fact.locator().startsWith(expected)
        || !fact.locator().substring(expected.length()).matches("0|[1-9][0-9]{0,8}")
        || !locators.add(fact.locator())
        || (fact.periodStart() != null && fact.periodStart().isAfter(fact.periodEnd()))
        || new BigDecimal(fact.value()).compareTo(new BigDecimal(fact.reportedValueRaw())) != 0) {
      throw new IllegalArgumentException(
          "Invalid selected fact semantics or duplicate source locator");
    }
  }

  private UUID concept(Fact fact, UUID evidence) {
    List<UUID> existing =
        jdbc.queryForList(
            "SELECT id FROM financial_concept WHERE taxonomy=? AND taxonomy_version IS NULL"
                + " AND concept_name=?",
            UUID.class,
            fact.taxonomy(),
            fact.concept());
    return existing.isEmpty()
        ? save(
                "financial-concepts",
                new FinancialConceptRequest(
                    Set.of(evidence),
                    fact.taxonomy(),
                    null,
                    fact.concept(),
                    fact.label(),
                    fact.description(),
                    "unknown"))
            .getId()
        : existing.getFirst();
  }

  private UUID filing(Fact fact, Target target, ProviderEntityBinding binding, UUID evidence) {
    List<UUID> existing =
        jdbc.queryForList(
            "SELECT id FROM filing WHERE provider_id=? AND company_id=? AND source_filing_id=?",
            UUID.class,
            binding.getProviderId(),
            binding.getCompanyId(),
            fact.accession());
    return existing.isEmpty()
        ? save(
                "filings",
                new FilingRequest(
                    Set.of(evidence),
                    binding.getCompanyId(),
                    binding.getProviderId(),
                    fact.accession(),
                    fact.form(),
                    fact.filedOn(),
                    null,
                    null,
                    null,
                    target.fetchUrl(),
                    null,
                    null,
                    "yes"))
            .getId()
        : existing.getFirst();
  }

  private String pointer(String value) {
    return value.replace("~", "~0").replace("/", "~1");
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private DomainRecord save(String resource, CreationRequest request) {
    return data.save(data.resource(resource), request);
  }
}

package com.fincore.server.service;

import com.fincore.server.dto.ingestion.IngestionContracts.Target;
import com.fincore.server.dto.ingestion.SecFinancialContracts;
import com.fincore.server.dto.ingestion.SecFinancialContracts.Refresh;
import com.fincore.server.dto.ingestion.SecFinancialContracts.Selection;
import com.fincore.server.dto.source.ProviderEntityBindingRequest;
import com.fincore.server.dto.source.SourceEndpointRequest;
import com.fincore.server.dto.source.SourceProviderRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.models.company.CompanyIdentifier;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Provisions a fixed SEC source from an existing CIK identity and queues selected raw facts. */
@Service
@Transactional
public class SecFinancialIngestionService {
  private final CompanyDataServiceInter data;
  private final IngestionJobService jobs;
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  /** Uses the existing domain APIs and one transaction for source setup and queueing. */
  public SecFinancialIngestionService(
      CompanyDataServiceInter data,
      IngestionJobService jobs,
      JdbcTemplate jdbc,
      ObjectMapper mapper) {
    this.data = data;
    this.jobs = jobs;
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  /** Fixed source operation; callers cannot supply arbitrary fetch destinations. */
  public static String fetchUrl(String cik) {
    return "https://data.sec.gov/api/xbrl/companyfacts/CIK" + cik + ".json";
  }

  /** Rejects duplicate or absent selections even for internally constructed target requests. */
  public static void validateSelection(Selection selection) {
    if (selection == null
        || selection.taxonomy() == null
        || !Set.of("us-gaap", "ifrs-full", "dei", "srt").contains(selection.taxonomy())
        || selection.concepts() == null
        || selection.concepts().isEmpty()
        || selection.concepts().size() > 20
        || selection.concepts().stream()
            .anyMatch(c -> c == null || !c.matches("[A-Za-z_][A-Za-z0-9_.-]{0,199}"))
        || new HashSet<>(selection.concepts()).size() != selection.concepts().size()) {
      throw new IllegalArgumentException(
          "Expected one taxonomy and 1–20 distinct raw concept names");
    }
  }

  /** Reuses the same manual-only target and unfinished job for an identical selection. */
  public Map<String, Object> refresh(Refresh request) {
    validateSelection(request.selection());
    data.load("companies", request.companyId());
    // Serializes this provisioner's shared provider/endpoint/binding/target lookup-and-create.
    jdbc.queryForList("SELECT pg_advisory_xact_lock(74000000000)");
    List<UUID> cikIds =
        jdbc.queryForList(
            "SELECT id FROM company_identifier WHERE company_id=? AND scheme='cik'"
                + " AND (valid_from IS NULL OR valid_from <= current_date)"
                + " AND (valid_to IS NULL OR valid_to >= current_date) ORDER BY id",
            UUID.class,
            request.companyId());
    List<CompanyIdentifier> identifiers =
        cikIds.stream()
            .map(id -> (CompanyIdentifier) data.load("company-identifiers", id))
            .toList();
    var ciks = identifiers.stream().map(CompanyIdentifier::getValue).distinct().toList();
    if (ciks.size() != 1 || !ciks.getFirst().matches("(?!0000000000)[0-9]{10}")) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Company needs one evidenced current CIK");
    }
    String cik = ciks.getFirst();
    Set<UUID> evidence =
        identifiers.stream()
            .flatMap(i -> i.getEvidenceIds().stream())
            .collect(java.util.stream.Collectors.toSet());
    List<UUID> providers =
        jdbc.queryForList("SELECT id FROM source_provider WHERE code='sec'", UUID.class);
    UUID provider =
        providers.isEmpty()
            ? data.save(
                    data.resource("providers"),
                    new SourceProviderRequest(
                        Set.of(), "sec", "SEC EDGAR", "regulator", "https://data.sec.gov"))
                .getId()
            : providers.getFirst();
    List<UUID> endpoints =
        jdbc.queryForList(
            "SELECT id FROM source_endpoint WHERE provider_id=? AND code='company-facts-selected'"
                + " AND contract_version='1'",
            UUID.class,
            provider);
    UUID endpoint =
        endpoints.isEmpty()
            ? data.save(
                    data.resource("source-endpoints"),
                    new SourceEndpointRequest(
                        Set.of(),
                        provider,
                        "company-facts-selected",
                        "1",
                        "https://www.sec.gov/search-filings/edgar-application-programming-interfaces",
                        "none",
                        null,
                        List.of("raw_financial_facts"),
                        "none",
                        "unknown",
                        Instant.now()))
                .getId()
            : endpoints.getFirst();
    List<UUID> bindings =
        jdbc.queryForList(
            "SELECT id FROM provider_entity_binding WHERE provider_id=? AND company_id=?"
                + " AND source_identifier=?"
                + " AND identifier_kind='cik' AND verification_status='verified'"
                + " AND valid_from IS NULL AND valid_to IS NULL ORDER BY id",
            UUID.class,
            provider,
            request.companyId(),
            cik);
    UUID binding =
        bindings.isEmpty()
            ? data.save(
                    data.resource("provider-bindings"),
                    new ProviderEntityBindingRequest(
                        evidence,
                        provider,
                        request.companyId(),
                        null,
                        null,
                        cik,
                        "cik",
                        "verified",
                        Instant.now(),
                        null,
                        null))
                .getId()
            : bindings.getFirst();
    Selection selected =
        new Selection(
            request.selection().taxonomy(),
            request.selection().concepts().stream().sorted().toList());
    List<UUID> targets =
        jdbc.queryForList(
            "SELECT id FROM ingestion_target WHERE binding_id=? AND endpoint_id=?"
                + " AND config->>'operation'='sec_company_facts'"
                + " AND config->'selection'=?::jsonb ORDER BY id",
            UUID.class,
            binding,
            endpoint,
            mapper.writeValueAsString(selected));
    UUID target =
        targets.isEmpty()
            ? (UUID)
                jobs.createTarget(
                        new Target(
                            binding,
                            endpoint,
                            fetchUrl(cik),
                            SecFinancialContracts.SOURCE,
                            SecFinancialContracts.PARSER_ID,
                            "SEC EDGAR",
                            "US",
                            "unknown",
                            null,
                            "sec_company_facts",
                            selected))
                    .get("id")
            : targets.getFirst();
    return Map.of("targetId", target, "selection", selected, "job", jobs.enqueue(target));
  }
}

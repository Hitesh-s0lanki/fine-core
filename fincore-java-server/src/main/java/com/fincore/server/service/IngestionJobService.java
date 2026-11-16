package com.fincore.server.service;

import com.fincore.server.dto.ingestion.IngestionContracts.Target;
import com.fincore.server.dto.ingestion.SecFinancialContracts;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.models.source.ProviderEntityBinding;
import com.fincore.server.models.source.SourceEndpoint;
import com.fincore.server.models.source.SourceProvider;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Durable scheduling, atomic claims and fenced leases, separate from append-only evidence. */
@Service
@Transactional
public class IngestionJobService {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final CompanyDataServiceInter data;

  /** Uses PostgreSQL row locks to coordinate API requests, schedulers and workers. */
  public IngestionJobService(JdbcTemplate jdbc, ObjectMapper mapper, CompanyDataServiceInter data) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.data = data;
  }

  /**
   * Creates an explicitly configured target for a verified company binding and unpaged endpoint.
   */
  public Map<String, Object> createTarget(Target target) {
    ProviderEntityBinding binding = binding(target);
    SourceEndpoint endpoint = (SourceEndpoint) data.load("source-endpoints", target.endpointId());
    URI url = URI.create(target.fetchUrl());
    if (!List.of("http", "https").contains(url.getScheme())
        || url.getHost() == null
        || url.getUserInfo() != null
        || url.getRawQuery() != null
        || url.getFragment() != null
        || !binding.getProviderId().equals(endpoint.getProviderId())
        || !"none".equals(endpoint.getPaginationKind())) {
      throw new IllegalArgumentException(
          "Expected a credential-free URL and unpaged provider endpoint");
    }
    if ("sec_company_facts".equals(target.operation())) {
      SecFinancialIngestionService.validateSelection(target.selection());
      SourceProvider provider = (SourceProvider) data.load("providers", binding.getProviderId());
      if (!"sec".equals(provider.getCode())
          || !"regulator".equals(provider.getKind())
          || !"cik".equals(binding.getIdentifierKind())
          || !binding.getSourceIdentifier().matches("(?!0000000000)[0-9]{10}")
          || !target
              .fetchUrl()
              .equals(SecFinancialIngestionService.fetchUrl(binding.getSourceIdentifier()))
          || !SecFinancialContracts.PARSER_ID.equals(target.adapterId())
          || !SecFinancialContracts.SOURCE.equals(target.sourceId())
          || !"company-facts-selected".equals(endpoint.getCode())
          || !"1".equals(endpoint.getContractVersion())) {
        throw new IllegalArgumentException(
            "Expected pinned SEC Company Facts source configuration");
      }
      List<UUID> owners =
          jdbc.queryForList(
              "SELECT DISTINCT company_id FROM company_identifier WHERE scheme='cik' AND value=?",
              UUID.class,
              binding.getSourceIdentifier());
      if (owners.size() != 1 || !owners.getFirst().equals(binding.getCompanyId())) {
        throw new IllegalArgumentException("SEC binding must resolve one evidenced CIK company");
      }
    } else if (!"company_profile".equals(target.operation()) || target.selection() != null) {
      throw new IllegalArgumentException("Unsupported operation or selection");
    }
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO ingestion_target(id, binding_id, endpoint_id, config, refresh_seconds)
        VALUES (?, ?, ?, ?::jsonb, ?)
        """,
        id,
        target.bindingId(),
        target.endpointId(),
        mapper.writeValueAsString(target),
        target.refreshSeconds());
    return Map.of("id", id, "target", target);
  }

  /** Reads a pinned target configuration. */
  public Target target(UUID id) {
    var rows =
        jdbc.queryForList(
            "SELECT config::text FROM ingestion_target WHERE id = ?", String.class, id);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion target not found");
    }
    return mapper.readValue(rows.getFirst(), Target.class);
  }

  /** Rechecks binding identity before dispatch and before committing any returned data. */
  public ProviderEntityBinding binding(Target target) {
    ProviderEntityBinding binding =
        (ProviderEntityBinding) data.load("provider-bindings", target.bindingId());
    if (binding.getCompanyId() == null || !"verified".equals(binding.getVerificationStatus())) {
      throw new IllegalArgumentException("A verified company binding is required");
    }
    return binding;
  }

  /** Enqueues at most one unfinished job per target, including concurrent refresh requests. */
  public Map<String, Object> enqueue(UUID targetId) {
    target(targetId);
    jdbc.queryForObject(
        "SELECT id FROM ingestion_target WHERE id = ? FOR UPDATE", UUID.class, targetId);
    UUID id =
        jdbc.queryForObject(
            """
        INSERT INTO ingestion_job(id, target_id) VALUES (?, ?)
        ON CONFLICT (target_id) WHERE status IN ('queued','running')
        DO UPDATE SET target_id = EXCLUDED.target_id
        RETURNING id
        """,
            UUID.class,
            UUID.randomUUID(),
            targetId);
    return get(id);
  }

  /** Schedules due targets; manual and periodic refreshes use the same queue. */
  public void scheduleDue() {
    List<UUID> due =
        jdbc.queryForList(
            """
        SELECT id FROM ingestion_target
        WHERE refresh_seconds IS NOT NULL AND next_run_at <= now()
        ORDER BY next_run_at LIMIT 100 FOR UPDATE SKIP LOCKED
        """,
            UUID.class);
    for (UUID id : due) {
      enqueue(id);
      jdbc.update(
          """
          UPDATE ingestion_target
          SET next_run_at = now() + refresh_seconds * interval '1 second' WHERE id = ?
          """,
          id);
    }
  }

  /** Claims one ready job or recovers an expired lease, with a fresh fencing token. */
  public Map<String, Object> claim() {
    return claim(null);
  }

  /** Claims only the requested job when a one-time caller supplies an ID. */
  public Map<String, Object> claim(UUID jobId) {
    jdbc.update(
        """
        UPDATE ingestion_job SET status = 'failed', lease_until = NULL,
        error_code = 'lease_expired', finished_at = now()
        WHERE status = 'running' AND lease_until < now() AND attempt >= 3
        """);
    List<UUID> ready =
        jdbc.queryForList(
            """
        SELECT id FROM ingestion_job
        WHERE ((status = 'queued' AND available_at <= now())
           OR (status = 'running' AND lease_until < now() AND attempt < 3))
          AND (?::uuid IS NULL OR id = ?)
        ORDER BY available_at, created_at LIMIT 1 FOR UPDATE SKIP LOCKED
        """,
            UUID.class,
            jobId,
            jobId);
    if (ready.isEmpty()) {
      return Map.of();
    }
    UUID id = ready.getFirst();
    jdbc.update(
        """
        UPDATE ingestion_job SET status = 'running', attempt = attempt + 1,
        lease_token = ?, lease_until = now() + interval '5 minutes', error_code = NULL
        WHERE id = ?
        """,
        UUID.randomUUID(),
        id);
    Map<String, Object> result = get(id);
    Target target = target((UUID) result.get("targetId"));
    ProviderEntityBinding binding = binding(target);
    var work = new java.util.LinkedHashMap<>(result);
    work.put("target", target);
    work.put("companyId", binding.getCompanyId());
    work.put("sourceIdentifier", binding.getSourceIdentifier());
    return work;
  }

  /** Returns operational status and the resulting snapshot, if one has been committed. */
  @Transactional(readOnly = true)
  public Map<String, Object> get(UUID id) {
    var rows =
        jdbc.query(
            "SELECT * FROM ingestion_job WHERE id = ?",
            (rs, row) -> {
              Map<String, Object> result = new java.util.LinkedHashMap<>();
              result.put("id", rs.getObject("id", UUID.class));
              result.put("targetId", rs.getObject("target_id", UUID.class));
              result.put("status", rs.getString("status"));
              result.put("attempt", rs.getInt("attempt"));
              result.put("leaseToken", rs.getObject("lease_token", UUID.class));
              result.put("snapshotId", rs.getObject("snapshot_id", UUID.class));
              result.put("errorCode", rs.getString("error_code"));
              String value = rs.getString("result");
              result.put("result", value == null ? null : mapper.readValue(value, Map.class));
              return result;
            },
            id);
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingestion job not found");
    }
    return rows.getFirst();
  }

  /** Locks a job and rejects stale workers; successful acknowledgement may be safely replayed. */
  public Map<String, Object> lock(UUID id, UUID token) {
    jdbc.queryForList("SELECT id FROM ingestion_job WHERE id = ? FOR UPDATE", id);
    Map<String, Object> job = get(id);
    if (!token.equals(job.get("leaseToken"))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Stale ingestion lease");
    }
    if ("succeeded".equals(job.get("status"))) {
      return job;
    }
    Boolean valid =
        jdbc.queryForObject(
            """
            SELECT status = 'running' AND lease_until > now() FROM ingestion_job WHERE id = ?
            """,
            Boolean.class,
            id);
    if (!Boolean.TRUE.equals(valid)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ingestion lease is not active");
    }
    return job;
  }

  /** Completes the job in the same transaction that commits its records and snapshot. */
  public void succeed(UUID id, UUID snapshot) {
    jdbc.update(
        """
        UPDATE ingestion_job SET status = 'succeeded', snapshot_id = ?,
        lease_until = NULL, finished_at = now() WHERE id = ?
        """,
        snapshot,
        id);
  }

  /** Records raw-fact completion without implying a selected financial snapshot. */
  public void succeedFinancial(UUID id, UUID run, int recordCount) {
    jdbc.update(
        "UPDATE ingestion_job SET status='succeeded', lease_until=NULL, finished_at=now(),"
            + " result=?::jsonb WHERE id=?",
        mapper.writeValueAsString(
            Map.of(
                "normalizationRunId",
                run,
                "recordCount",
                recordCount,
                "coverage",
                "selected_raw_facts",
                "parserVersion",
                SecFinancialContracts.VERSION)),
        id);
  }

  /** Applies bounded backoff only to transient errors; semantic errors require review. */
  public void fail(UUID id, String code) {
    jdbc.update(
        """
        UPDATE ingestion_job SET
          status = CASE WHEN ? AND attempt < 3 THEN 'queued' ELSE 'failed' END,
          available_at = now() + (30 * power(2, attempt - 1)) * interval '1 second',
          lease_until = NULL, error_code = ?,
          finished_at = CASE WHEN ? AND attempt < 3 THEN NULL ELSE now() END
        WHERE id = ?
        """,
        List.of("timeout", "network", "rate_limited", "provider_error").contains(code),
        code,
        List.of("timeout", "network", "rate_limited", "provider_error").contains(code),
        id);
  }
}

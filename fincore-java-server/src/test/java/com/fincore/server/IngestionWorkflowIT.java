package com.fincore.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.fincore.server.interfaces.SnapshotServiceInter;
import com.fincore.server.service.IngestionJobService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Exercises durable job coordination and atomic profile commits against real PostgreSQL. */
class IngestionWorkflowIT extends CompanyApiTestSupport {
  @MockitoSpyBean SnapshotServiceInter snapshots;
  @Autowired IngestionJobService jobs;

  private static final String BASE = "/api/v1/ingestion";

  @AfterEach
  void finishUnclaimedJobs() {
    jdbc.update(
        """
        UPDATE ingestion_target SET refresh_seconds = NULL WHERE binding_id IN (
          SELECT id FROM provider_entity_binding WHERE company_id = ?)
        """,
        company);
    jdbc.update(
        """
        UPDATE ingestion_job SET status = 'failed', lease_until = NULL
        WHERE status IN ('queued','running') AND target_id IN (
          SELECT t.id FROM ingestion_target t JOIN provider_entity_binding b ON b.id = t.binding_id
          WHERE b.company_id = ?)
        """,
        company);
  }

  @Test
  void duplicateRefreshAndCompletionAreIdempotentAndOverviewIsFrozen() throws Exception {
    UUID target = target(60);
    var queued = json(request("POST", BASE + "/targets/" + target + "/refresh", Map.of()));
    UUID job = UUID.fromString(queued.get("id").asText());
    assertThat(json(request("POST", BASE + "/targets/" + target + "/refresh", Map.of())))
        .isEqualTo(queued);
    assertThat(
            request("POST", BASE + "/jobs/claim?jobId=" + UUID.randomUUID(), Map.of()).statusCode())
        .isEqualTo(204);
    var claim = json(request("POST", BASE + "/jobs/claim?jobId=" + job, Map.of()));
    assertThat(claim.get("id").asText()).isEqualTo(job.toString());
    assertThat(request("POST", BASE + "/jobs/claim", Map.of()).statusCode()).isEqualTo(204);
    var input = completion(UUID.fromString(claim.get("leaseToken").asText()));
    var completed = request("POST", BASE + "/jobs/" + job + "/complete", input);
    assertThat(completed.statusCode()).withFailMessage(completed.body()).isEqualTo(200);
    var result = json(completed);
    assertThat(result.get("status").asText()).isEqualTo("succeeded");
    assertThat(json(request("POST", BASE + "/jobs/" + job + "/complete", input))).isEqualTo(result);
    var overview =
        request(
            "GET",
            "/api/v1/companies/"
                + company
                + "/overview?snapshotId="
                + result.get("snapshotId").asText(),
            null);
    assertThat(overview.statusCode()).isEqualTo(200);
    assertThat(overview.body()).contains("Example Issuer", "profile-only-v1", "FC001");
    assertThat(
            jdbc.queryForObject(
                """
                SELECT count(*) FROM normalization_run WHERE ingestion_attempt_id IN (
                  SELECT id FROM ingestion_attempt WHERE company_id = ?)
                """,
                Integer.class,
                company))
        .isEqualTo(1);
    // A second refresh produces a new snapshot without changing the first one.
    request("POST", BASE + "/targets/" + target + "/refresh", Map.of());
    var second = json(request("POST", BASE + "/jobs/claim", Map.of()));
    var changed = completion(UUID.fromString(second.get("leaseToken").asText()));
    changed.put(
        "records",
        List.of(
            body(
                "recordType",
                "company_profile",
                "name",
                "Changed Name",
                "registrationNumber",
                "FC001")));
    assertThat(
            request("POST", BASE + "/jobs/" + second.get("id").asText() + "/complete", changed)
                .statusCode())
        .isEqualTo(200);
    assertThat(
            request(
                    "GET",
                    "/api/v1/companies/"
                        + company
                        + "/overview?snapshotId="
                        + result.get("snapshotId").asText(),
                    null)
                .body())
        .isEqualTo(overview.body());
  }

  @Test
  void claimsAreExclusiveAndExpiredWorkersCannotCommit() throws Exception {
    UUID target = target(null);
    request("POST", BASE + "/targets/" + target + "/refresh", Map.of());
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first = pool.submit(() -> request("POST", BASE + "/jobs/claim", Map.of()));
      var second = pool.submit(() -> request("POST", BASE + "/jobs/claim", Map.of()));
      var a = first.get();
      var b = second.get();
      assertThat(List.of(a.statusCode(), b.statusCode())).containsExactlyInAnyOrder(200, 204);
      var claimed = json(a.statusCode() == 200 ? a : b);
      UUID job = UUID.fromString(claimed.get("id").asText());
      UUID stale = UUID.fromString(claimed.get("leaseToken").asText());
      jdbc.update(
          "UPDATE ingestion_job SET lease_until = now() - interval '1 second' WHERE id = ?", job);
      var recovered = json(request("POST", BASE + "/jobs/claim", Map.of()));
      assertThat(recovered.get("attempt").asInt()).isEqualTo(2);
      assertThat(recovered.get("leaseToken").asText()).isNotEqualTo(stale.toString());
      assertThat(
              request("POST", BASE + "/jobs/" + job + "/complete", completion(stale)).statusCode())
          .isEqualTo(409);
      var bad = completion(UUID.fromString(recovered.get("leaseToken").asText()));
      bad.put(
          "records",
          List.of(
              body(
                  "recordType",
                  "company_profile",
                  "name",
                  "Wrong Company",
                  "registrationNumber",
                  "OTHER")));
      assertThat(request("POST", BASE + "/jobs/" + job + "/complete", bad).statusCode())
          .isEqualTo(400);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM ingestion_attempt WHERE company_id = ?",
                  Integer.class,
                  company))
          .isZero();
      request(
          "POST",
          BASE + "/jobs/" + job + "/fail",
          failure(recovered.get("leaseToken").asText(), "normalization"));
    }
  }

  @Test
  void transientFailuresBackOffAndStopAfterThreeAttempts() throws Exception {
    UUID target = target(null);
    var job =
        json(request("POST", BASE + "/targets/" + target + "/refresh", Map.of()))
            .get("id")
            .asText();
    for (int attempt = 1; attempt <= 3; attempt++) {
      var claim = json(request("POST", BASE + "/jobs/claim", Map.of()));
      var failed =
          json(
              request(
                  "POST",
                  BASE + "/jobs/" + job + "/fail",
                  failure(claim.get("leaseToken").asText(), "rate_limited")));
      assertThat(failed.get("status").asText()).isEqualTo(attempt == 3 ? "failed" : "queued");
      assertThat(request("POST", BASE + "/jobs/claim", Map.of()).statusCode()).isEqualTo(204);
      jdbc.update(
          "UPDATE ingestion_job SET available_at = now() WHERE id = ?", UUID.fromString(job));
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ingestion_attempt WHERE company_id = ?",
                Integer.class,
                company))
        .isEqualTo(3);
  }

  @Test
  void lateConstraintFailureRollsBackAllDomainWrites() throws Exception {
    UUID target = target(null);
    var job =
        json(request("POST", BASE + "/targets/" + target + "/refresh", Map.of()))
            .get("id")
            .asText();
    var claim = json(request("POST", BASE + "/jobs/claim", Map.of()));
    var bad = completion(UUID.fromString(claim.get("leaseToken").asText()));
    int before = jdbc.queryForObject("SELECT count(*) FROM data_record", Integer.class);
    doThrow(new IllegalArgumentException("Snapshot selection rejected"))
        .when(snapshots)
        .create(eq(company), any());
    assertThat(request("POST", BASE + "/jobs/" + job + "/complete", bad).statusCode())
        .isEqualTo(400);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ingestion_attempt WHERE company_id = ?",
                Integer.class,
                company))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM data_record", Integer.class))
        .isEqualTo(before);
    request(
        "POST",
        BASE + "/jobs/" + job + "/fail",
        failure(claim.get("leaseToken").asText(), "configuration"));
  }

  @Test
  void dueSchedulingSharesQueueAndDoesNotEnqueueManualOnlyTargets() throws Exception {
    UUID periodic = target(60);
    jobs.scheduleDue();
    jobs.scheduleDue();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ingestion_job WHERE target_id = ?", Integer.class, periodic))
        .isEqualTo(1);
    UUID manual = target(null);
    jobs.scheduleDue();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ingestion_job WHERE target_id = ?", Integer.class, manual))
        .isZero();
  }

  private UUID target(Integer seconds) throws Exception {
    UUID endpoint =
        create(
            "source-endpoints",
            body(
                "evidenceIds",
                List.of(),
                "providerId",
                provider,
                "code",
                "profile-" + UUID.randomUUID(),
                "contractVersion",
                "1",
                "authScheme",
                "none",
                "supportedDomains",
                List.of("profile"),
                "paginationKind",
                "none",
                "accessStatus",
                "available",
                "checkedAt",
                Instant.now().toString()));
    UUID binding =
        create(
            "provider-bindings",
            body(
                "evidenceIds",
                List.of(evidence),
                "providerId",
                provider,
                "companyId",
                company,
                "sourceIdentifier",
                "FC001",
                "identifierKind",
                "registry_number",
                "verificationStatus",
                "verified",
                "verifiedAt",
                Instant.now().toString()));
    var response =
        request(
            "POST",
            BASE + "/targets",
            body(
                "bindingId",
                binding,
                "endpointId",
                endpoint,
                "fetchUrl",
                "http://127.0.0.1:9999/profile",
                "sourceId",
                "test_profile",
                "adapterId",
                "a".repeat(64),
                "registry",
                "Example Registry",
                "registryJurisdiction",
                "GB",
                "registrationKind",
                "overseas",
                "refreshSeconds",
                seconds));
    assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
    return UUID.fromString(json(response).get("id").asText());
  }

  private Map<String, Object> completion(UUID lease) {
    return body(
        "leaseToken",
        lease,
        "requestedAt",
        Instant.now().minusSeconds(1).toString(),
        "retrievedAt",
        Instant.now().toString(),
        "adapterId",
        "a".repeat(64),
        "sourceId",
        "test_profile",
        "artifact",
        body(
            "storageUri",
            "fixture:" + UUID.randomUUID(),
            "sha256",
            "b".repeat(64),
            "byteLength",
            10,
            "contentType",
            "application/json",
            "storedAt",
            Instant.now().toString()),
        "records",
        List.of(
            body(
                "recordType",
                "company_profile",
                "name",
                "Example Issuer",
                "registrationNumber",
                "FC001",
                "status",
                "Active",
                "jurisdiction",
                "IN")));
  }

  private Map<String, Object> failure(String lease, String error) {
    return body(
        "leaseToken",
        lease,
        "errorCode",
        error,
        "requestedAt",
        Instant.now().minusSeconds(1).toString(),
        "retrievedAt",
        Instant.now().toString(),
        "httpStatus",
        429);
  }
}

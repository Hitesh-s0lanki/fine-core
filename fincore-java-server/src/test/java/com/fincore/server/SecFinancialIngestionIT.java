package com.fincore.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.fincore.server.dto.ingestion.SecFinancialContracts;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class SecFinancialIngestionIT extends CompanyApiTestSupport {
  private static final String BASE = "/api/v1/ingestion";
  private String cik;

  @AfterEach
  void finishUnclaimedJobs() {
    jdbc.update(
        "UPDATE ingestion_target SET refresh_seconds=NULL WHERE binding_id IN"
            + " (SELECT id FROM provider_entity_binding WHERE company_id=?)",
        company);
    jdbc.update(
        "UPDATE ingestion_job SET status='failed', lease_until=NULL"
            + " WHERE status IN ('queued','running') AND target_id IN"
            + " (SELECT t.id FROM ingestion_target t JOIN provider_entity_binding b"
            + " ON b.id=t.binding_id WHERE b.company_id=?)",
        company);
  }

  @BeforeEach
  void cikFixture() throws Exception {
    cik =
        Long.toString(
            7000000000L + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1000000000L));
    create(
        "company-identifiers",
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            company,
            "scheme",
            "cik",
            "authority",
            "SEC",
            "value",
            cik));
  }

  @Test
  void queuesSelectedFactsAndCommitsExactSourcedObservationsWithoutSnapshot() throws Exception {
    JsonNode refresh = refresh();
    assertThat(refresh.get("selection").get("concepts").toString())
        .isEqualTo("[\"Assets\",\"Revenue\"]");
    JsonNode again = refresh();
    assertThat(again.at("/job/id")).isEqualTo(refresh.at("/job/id"));
    JsonNode claim = claim(refresh);
    assertThat(claim.at("/target/operation").asText()).isEqualTo("sec_company_facts");
    assertThat(claim.at("/target/fetchUrl").asText())
        .isEqualTo("https://data.sec.gov/api/xbrl/companyfacts/CIK" + cik + ".json");
    Map<String, Object> completion = completion(claim);
    var response =
        request(
            "POST",
            BASE + "/jobs/" + claim.get("id").asText() + "/complete-financials",
            completion);
    assertThat(response.statusCode()).isEqualTo(200);
    JsonNode result = json(response);
    assertThat(result.get("status").asText()).isEqualTo("succeeded");
    assertThat(result.get("snapshotId").isNull()).isTrue();
    assertThat(result.at("/result/recordCount").asInt()).isEqualTo(2);
    assertThat(result.at("/result/coverage").asText()).isEqualTo("selected_raw_facts");
    UUID run = UUID.fromString(result.at("/result/normalizationRunId").asText());
    JsonNode normalized =
        json(request("GET", "/api/v1/company-data/normalization-runs/" + run, null));
    assertThat(normalized.at("/attributes/adapterContentHash").asText())
        .isEqualTo(SecFinancialContracts.PARSER_ID);
    assertThat(normalized.at("/attributes/outputRecordIds").size()).isEqualTo(2);
    JsonNode facts =
        json(
            request(
                "GET", "/api/v1/company-data/financial-observations?companyId=" + company, null));
    assertThat(facts.get("items").size()).isEqualTo(2);
    JsonNode first = facts.get("items").get(0);
    assertThat(first.at("/attributes/value").asText()).isEqualTo("123450.000000000000000000");
    assertThat(first.at("/attributes/reportedValueRaw").asText()).isEqualTo("1.2345e+5");
    assertThat(first.at("/attributes/scope").asText()).isEqualTo("unspecified");
    assertThat(first.at("/attributes/dimensionsStatus").asText()).isEqualTo("not_reported");
    assertThat(first.at("/attributes/reportedDecimals").isNull()).isTrue();
    assertThat(first.at("/attributes/entityIdentifier").asText()).isEqualTo(cik);
    JsonNode source =
        json(
            request(
                "GET",
                "/api/v1/company-data/source-references/"
                    + first.get("evidenceIds").get(0).asText(),
                null));
    assertThat(source.at("/attributes/locator").asText())
        .isEqualTo("/facts/us-gaap/Assets/units/USD/0");
    assertThat(source.at("/attributes/sourceUrl").asText())
        .isEqualTo(claim.at("/target/fetchUrl").asText());
    JsonNode filing =
        json(
            request(
                "GET",
                "/api/v1/company-data/filings/" + first.at("/attributes/filingId").asText(),
                null));
    assertThat(filing.at("/attributes/sourceFilingId").asText()).isEqualTo("0001193125-25-000001");
    assertThat(filing.at("/attributes/filingDate").asText()).isEqualTo("2025-10-31");
    assertThat(filing.at("/attributes/reportDate").isNull()).isTrue();
    JsonNode artifact =
        json(
            request(
                "GET",
                "/api/v1/company-data/raw-artifacts/"
                    + source.at("/attributes/artifactId").asText(),
                null));
    assertThat(artifact.at("/attributes/hashBasis").asText()).isEqualTo("response_bytes");
    final int before = records();
    assertThat(
            request(
                    "POST",
                    BASE + "/jobs/" + claim.get("id").asText() + "/complete-financials",
                    completion)
                .statusCode())
        .isEqualTo(200);
    assertThat(records()).isEqualTo(before);
    // A later refresh appends a new evidence-scoped assertion set while reusing concepts and
    // accessions.
    JsonNode secondClaim = claim(refresh());
    assertThat(
            request(
                    "POST",
                    BASE + "/jobs/" + secondClaim.get("id").asText() + "/complete-financials",
                    completion(secondClaim))
                .statusCode())
        .isEqualTo(200);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM financial_observation WHERE company_id=?",
                Integer.class,
                company))
        .isEqualTo(4);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM filing WHERE company_id=?", Integer.class, company))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM company_snapshot WHERE company_id=?", Integer.class, company))
        .isZero();
  }

  @Test
  void concurrentRefreshSetupReusesOneTargetAndJob() {
    var first = CompletableFuture.supplyAsync(this::refreshUnchecked);
    var second = CompletableFuture.supplyAsync(this::refreshUnchecked);
    assertThat(first.join().at("/job/id")).isEqualTo(second.join().at("/job/id"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ingestion_target t JOIN provider_entity_binding b"
                    + " ON b.id=t.binding_id WHERE b.company_id=?",
                Integer.class,
                company))
        .isEqualTo(1);
  }

  @Test
  void rejectsMismatchedIdentitySelectionAndDuplicateLocatorsBeforeWriting() throws Exception {
    JsonNode claim = claim(refresh());
    final int before = records();
    var badIdentity = completion(claim);
    badIdentity.put("cik", "0000000001");
    assertBad(claim, badIdentity);
    var badSelection = completion(claim);
    badSelection.put("records", List.of(fact("Assets", "USD", "1.2345e+5", "123450")));
    assertBad(claim, badSelection);
    var duplicate = completion(claim);
    duplicate.put(
        "records",
        List.of(
            fact("Assets", "USD", "1.2345e+5", "123450"),
            fact("Assets", "USD", "1.2345e+5", "123450")));
    assertBad(claim, duplicate);
    var mismatch = completion(claim);
    mismatch.put(
        "records", List.of(fact("Assets", "USD", "1", "2"), fact("Revenue", "USD", "1", "1")));
    assertBad(claim, mismatch);
    assertThat(records()).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM ingestion_attempt WHERE company_id=?",
                Integer.class,
                company))
        .isZero();
  }

  @Test
  void lateFilingConflictRollsBackEarlierFactsEvidenceAndAttempt() throws Exception {
    JsonNode claim = claim(refresh());
    var completion = completion(claim);
    Map<String, Object> second = fact("Revenue", "USD", "1", "1");
    second.put("form", "10-Q");
    completion.put("records", List.of(fact("Assets", "USD", "1", "1"), second));
    final int before = records();
    assertBad(claim, completion);
    assertThat(records()).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM financial_observation WHERE company_id=?",
                Integer.class,
                company))
        .isZero();
    assertThat(
            json(request("GET", BASE + "/jobs/" + claim.get("id").asText(), null))
                .get("status")
                .asText())
        .isEqualTo("running");
  }

  @Test
  void financialFailureUsesExistingRetryPolicyAndRejectsStaleLease() throws Exception {
    JsonNode claim = claim(refresh());
    UUID job = UUID.fromString(claim.get("id").asText());
    var failure =
        body(
            "leaseToken",
            claim.get("leaseToken").asText(),
            "errorCode",
            "rate_limited",
            "requestedAt",
            Instant.now().toString(),
            "retrievedAt",
            Instant.now().toString(),
            "httpStatus",
            429);
    assertThat(
            json(request("POST", BASE + "/jobs/" + job + "/fail", failure)).get("status").asText())
        .isEqualTo("queued");
    jdbc.update("UPDATE ingestion_job SET available_at=now() WHERE id=?", job);
    JsonNode recovered = json(request("POST", BASE + "/jobs/claim?jobId=" + job, Map.of()));
    assertThat(
            request("POST", BASE + "/jobs/" + job + "/complete-financials", completion(claim))
                .statusCode())
        .isEqualTo(409);
    assertThat(recovered.get("attempt").asInt()).isEqualTo(2);
    var invalid =
        body(
            "leaseToken",
            recovered.get("leaseToken").asText(),
            "errorCode",
            "invalid_payload",
            "requestedAt",
            Instant.now().toString(),
            "retrievedAt",
            Instant.now().toString(),
            "httpStatus",
            200);
    assertThat(
            json(request("POST", BASE + "/jobs/" + job + "/fail", invalid)).get("status").asText())
        .isEqualTo("failed");
  }

  @Test
  void rejectsInvalidSelectionAndCompaniesWithoutOneCurrentCik() throws Exception {
    assertThat(
            request(
                    "POST",
                    BASE + "/sec-company-facts/refresh",
                    body(
                        "companyId",
                        company,
                        "selection",
                        body("taxonomy", "us-gaap", "concepts", List.of("Assets", "Assets"))))
                .statusCode())
        .isEqualTo(400);
    create(
        "company-identifiers",
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            company,
            "scheme",
            "cik",
            "authority",
            "SEC-extra",
            "value",
            "0000000001"));
    assertThat(request("POST", BASE + "/sec-company-facts/refresh", refreshBody()).statusCode())
        .isEqualTo(409);
  }

  @Test
  void completionEndpointsCannotCrossProfileAndFinancialOperations() throws Exception {
    JsonNode financialClaim = claim(refresh());
    var profileCompletion = completion(financialClaim);
    profileCompletion.remove("cik");
    profileCompletion.put(
        "records",
        List.of(
            body("recordType", "company_profile", "name", "Issuer", "registrationNumber", cik)));
    final int before = records();
    assertThat(
            request(
                    "POST",
                    BASE + "/jobs/" + financialClaim.get("id").asText() + "/complete",
                    profileCompletion)
                .statusCode())
        .isEqualTo(400);
    assertThat(records()).isEqualTo(before);
    @SuppressWarnings("unchecked")
    Map<String, Object> profileTarget =
        mapper.convertValue(financialClaim.get("target"), Map.class);
    profileTarget.put("operation", "company_profile");
    profileTarget.remove("selection");
    JsonNode target = json(request("POST", BASE + "/targets", profileTarget));
    JsonNode refresh =
        json(
            request("POST", BASE + "/targets/" + target.get("id").asText() + "/refresh", Map.of()));
    JsonNode profileClaim =
        json(request("POST", BASE + "/jobs/claim?jobId=" + refresh.get("id").asText(), Map.of()));
    final int afterTarget = records();
    assertBad(profileClaim, completion(profileClaim));
    assertThat(records()).isEqualTo(afterTarget);
  }

  private JsonNode refresh() throws Exception {
    var response = request("POST", BASE + "/sec-company-facts/refresh", refreshBody());
    assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(200);
    return json(response);
  }

  private JsonNode refreshUnchecked() {
    try {
      return refresh();
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }

  private Map<String, Object> refreshBody() {
    return body(
        "companyId",
        company,
        "selection",
        body("taxonomy", "us-gaap", "concepts", List.of("Revenue", "Assets")));
  }

  private JsonNode claim(JsonNode refresh) throws Exception {
    var response =
        request("POST", BASE + "/jobs/claim?jobId=" + refresh.at("/job/id").asText(), Map.of());
    assertThat(response.statusCode()).isEqualTo(200);
    return json(response);
  }

  private Map<String, Object> completion(JsonNode claim) {
    Instant now = Instant.now();
    return body(
        "leaseToken",
        claim.get("leaseToken").asText(),
        "requestedAt",
        now.toString(),
        "retrievedAt",
        now.toString(),
        "artifact",
        body(
            "storageUri",
            "file:///test/" + UUID.randomUUID(),
            "sha256",
            "b".repeat(64),
            "byteLength",
            100,
            "contentType",
            "application/json",
            "storedAt",
            now.toString()),
        "adapterId",
        SecFinancialContracts.PARSER_ID,
        "sourceId",
        SecFinancialContracts.SOURCE,
        "cik",
        cik,
        "records",
        List.of(
            fact("Assets", "USD", "1.2345e+5", "123450"),
            fact("Revenue", "USD/shares", "123.4500", "123.4500")));
  }

  private Map<String, Object> fact(String concept, String unit, String token, String value) {
    var fact =
        body(
            "taxonomy",
            "us-gaap",
            "concept",
            concept,
            "label",
            concept,
            "description",
            "Source concept",
            "unit",
            unit,
            "value",
            value,
            "reportedValueRaw",
            token,
            "periodEnd",
            "2025-09-30",
            "fiscalYear",
            2025,
            "fiscalPeriod",
            "FY",
            "frame",
            "CY2025",
            "accession",
            "0001193125-25-000001",
            "form",
            "10-K",
            "filedOn",
            "2025-10-31",
            "locator",
            "/facts/us-gaap/" + concept + "/units/" + unit.replace("/", "~1") + "/0");
    if (concept.equals("Revenue")) {
      fact.put("periodStart", "2024-10-01");
    }
    return fact;
  }

  private void assertBad(JsonNode claim, Map<String, Object> completion) throws Exception {
    assertThat(
            request(
                    "POST",
                    BASE + "/jobs/" + claim.get("id").asText() + "/complete-financials",
                    completion)
                .statusCode())
        .isEqualTo(400);
  }

  private int records() {
    return jdbc.queryForObject("SELECT count(*) FROM data_record", Integer.class);
  }
}

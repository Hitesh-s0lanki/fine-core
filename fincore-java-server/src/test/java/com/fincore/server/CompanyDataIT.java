package com.fincore.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CompanyDataIT extends CompanyApiTestSupport {
  @Test
  void migrationsCreateAllCanonicalTablesAndValidateJpa() {
    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class))
        .isPositive();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema='public'"
                    + " and table_name='financial_observation'",
                Integer.class))
        .isEqualTo(1);
  }

  @Test
  void macroSeriesUsesCanonicalRoute() throws Exception {
    assertThat(request("GET", "/api/v1/company-data/macro-series", null).statusCode())
        .isEqualTo(200);
    assertThat(request("GET", "/api/v1/company-data/macro-seriess", null).statusCode())
        .isEqualTo(404);
  }

  @Test
  void snapshotCannotIncludeAnotherCompanysRelationship() throws Exception {
    UUID other =
        UUID.fromString(
            json(request(
                    "POST",
                    "/api/v1/companies",
                    body(
                        "company",
                        body(
                            "evidenceIds",
                            List.of(evidence),
                            "slug",
                            "other-" + UUID.randomUUID(),
                            "displayName",
                            "Other",
                            "entityKind",
                            "issuer"),
                        "identifiers",
                        List.of(
                            body(
                                "evidenceIds",
                                List.of(evidence),
                                "scheme",
                                "provider_company_id",
                                "authority",
                                "fixture",
                                "value",
                                UUID.randomUUID().toString())))))
                .get("id")
                .asText());
    UUID relationship =
        create(
            "company-relationships",
            body(
                "evidenceIds",
                List.of(evidence),
                "subjectCompanyId",
                other,
                "objectCompanyId",
                company,
                "relationshipType",
                "subsidiary"));
    var selected = List.of(member(company, "identity"), member(relationship, "relationship"));
    assertThat(
            request(
                    "POST",
                    "/api/v1/companies/" + company + "/snapshots",
                    snapshotFixture(selected, List.of()))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            request(
                    "POST",
                    "/api/v1/companies/" + other + "/snapshots",
                    snapshotFixture(
                        List.of(member(other, "identity"), member(relationship, "relationship")),
                        List.of()))
                .statusCode())
        .isEqualTo(201);
  }

  @Test
  void onboardingSearchesIdentifiersPreservingLeadingZeros() throws Exception {
    String cik =
        "0"
            + String.format(
                "%09d", Math.abs(UUID.randomUUID().getLeastSignificantBits() % 1000000000));
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
            "sec",
            "value",
            cik));
    var response = request("GET", "/api/v1/companies?query=" + cik, null);
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).get("items").get(0).get("id").asText()).isEqualTo(company.toString());
    assertThat(request("GET", "/api/v1/companies?size=101", null).statusCode()).isEqualTo(400);
  }

  @Test
  void failedIdentifierRollsBackCompanyOnboarding() throws Exception {
    String slug = "rollback-" + UUID.randomUUID();
    var response =
        request(
            "POST",
            "/api/v1/companies",
            body(
                "company",
                body(
                    "evidenceIds",
                    List.of(evidence),
                    "slug",
                    slug,
                    "displayName",
                    "Rollback",
                    "entityKind",
                    "issuer"),
                "identifiers",
                List.of(
                    body(
                        "evidenceIds",
                        List.of(evidence),
                        "scheme",
                        "cik",
                        "authority",
                        "sec",
                        "value",
                        "123"))));
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(
            jdbc.queryForObject("select count(*) from company where slug = ?", Integer.class, slug))
        .isZero();
  }

  @Test
  void profileRejectsUnstructuredUnknownAndInvalidValues() throws Exception {
    var profile =
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            company,
            "field",
            "fiscal_year_end",
            "profileValue",
            body("month", 13, "day", 31));
    assertThat(post("profile-observations", profile).statusCode()).isEqualTo(400);
    profile.put("profileValue", body("month", 3, "day", 31));
    assertThat(post("profile-observations", profile).statusCode()).isEqualTo(201);
    profile.put("profileValue", body("month", 3, "day", 31, "madeUp", true));
    assertThat(post("profile-observations", profile).statusCode()).isEqualTo(400);
  }

  @Test
  void foreignReferencesAndCrossCompanySecurityAreRejected() throws Exception {
    UUID otherSecurity =
        create(
            "securities",
            body(
                "evidenceIds",
                List.of(evidence),
                "companyId",
                company,
                "securityType",
                "ordinary_share"));
    var announcement =
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            UUID.randomUUID(),
            "providerId",
            provider,
            "sourceRecordId",
            "a",
            "title",
            "Test",
            "publishedAtRaw",
            "raw");
    assertThat(post("announcements", announcement).statusCode()).isEqualTo(400);
    UUID listing =
        create(
            "listings",
            body(
                "evidenceIds",
                List.of(evidence),
                "securityId",
                otherSecurity,
                "exchangeCode",
                "NSE",
                "symbol",
                "S" + UUID.randomUUID(),
                "status",
                "active"));
    announcement.put("companyId", company);
    announcement.put("listingId", listing);
    assertThat(post("announcements", announcement).statusCode()).isEqualTo(201);
  }

  @Test
  void financialDecimalAndDurationAreValidatedWithoutFloatCoercion() throws Exception {
    Map<String, Object> fact = financialFixture();
    var response = post("financial-observations", fact);
    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(json(response).at("/attributes/value").isString()).isTrue();
    assertThat(json(response).at("/attributes/value").asText()).isEqualTo("0.123456789012345678");
    fact.put("assertionKey", UUID.randomUUID().toString());
    fact.put("value", 0.125);
    assertThat(post("financial-observations", fact).statusCode()).isEqualTo(400);
    fact.put("value", "0.1234567890123456789");
    assertThat(post("financial-observations", fact).statusCode()).isEqualTo(400);
    fact.put("value", "1.25");
    fact.put("periodStart", "2026-01-01");
    assertThat(post("financial-observations", fact).statusCode()).isEqualTo(400);
  }

  @Test
  void identifierIntervalsConflictAndDoNotEraseEarlierRows() throws Exception {
    var identity =
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            company,
            "scheme",
            "provider_company_id",
            "authority",
            "overlap",
            "value",
            UUID.randomUUID().toString(),
            "validFrom",
            "2020-01-01",
            "validTo",
            "2025-12-31");
    assertThat(post("company-identifiers", identity).statusCode()).isEqualTo(201);
    identity.put("validFrom", "2024-01-01");
    assertThat(post("company-identifiers", identity).statusCode()).isEqualTo(409);
    identity.put("validFrom", "2026-01-01");
    identity.put("validTo", "2027-12-31");
    assertThat(post("company-identifiers", identity).statusCode()).isEqualTo(201);
  }

  @Test
  void sourceFailuresRemainEvidenceWithoutCreatingCompanyNumbers() throws Exception {
    UUID endpoint =
        create(
            "source-endpoints",
            body(
                "evidenceIds",
                List.of(),
                "providerId",
                provider,
                "code",
                "quote",
                "contractVersion",
                "1",
                "authScheme",
                "api_key",
                "supportedDomains",
                List.of("prices"),
                "paginationKind",
                "none",
                "accessStatus",
                "blocked",
                "checkedAt",
                "2026-10-22T12:00:00Z"));
    var attempt =
        body(
            "evidenceIds",
            List.of(),
            "providerId",
            provider,
            "sourceEndpointId",
            endpoint,
            "companyId",
            company,
            "endpoint",
            "quote",
            "requestUrlRedacted",
            "https://example.com/quote",
            "requestedAt",
            "2026-10-22T12:00:00Z",
            "httpStatus",
            403,
            "outcome",
            "forbidden",
            "requestScope",
            body());
    assertThat(post("ingestion-attempts", attempt).statusCode()).isEqualTo(201);
    assertThat(
            post(
                    "market-quotes",
                    body(
                        "evidenceIds",
                        List.of(evidence),
                        "listingId",
                        UUID.randomUUID(),
                        "asOf",
                        "2026-10-22T12:00:00Z",
                        "currency",
                        "USD",
                        "lastPrice",
                        "0"))
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void snapshotIsFrozenAndReplayDoesNotReadLaterProfiles() throws Exception {
    UUID profile =
        create(
            "profile-observations",
            body(
                "evidenceIds",
                List.of(evidence),
                "companyId",
                company,
                "field",
                "description",
                "profileValue",
                "Original business"));
    var request =
        snapshotFixture(
            List.of(member(company, "identity"), member(profile, "profile")), List.of());
    var response = request("POST", "/api/v1/companies/" + company + "/snapshots", request);
    assertThat(response.statusCode()).isEqualTo(201);
    UUID snapshot = UUID.fromString(json(response).get("id").asText());
    String url = "/api/v1/companies/" + company + "/overview?snapshotId=" + snapshot;
    JsonNode before = json(request("GET", url, null));
    create(
        "profile-observations",
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            company,
            "field",
            "description",
            "profileValue",
            "New business"));
    assertThat(json(request("GET", url, null))).isEqualTo(before);
    assertThat(post("snapshot-members", body()).statusCode()).isEqualTo(400);
    assertThatThrownBy(
            () -> jdbc.update("update company set display_name='changed' where id=?", company))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    UUID laterEvidence =
        create(
            "source-references",
            body(
                "evidenceIds",
                List.of(),
                "extractionMethod",
                "configured",
                "observedAt",
                "2026-10-23T12:00:00Z"));
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "insert into record_evidence(record_id,evidence_id) values (?,?)",
                    company,
                    laterEvidence))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
        .hasMessageContaining("Record evidence is frozen");
    assertThat(json(request("GET", url, null))).isEqualTo(before);
  }

  @Test
  void snapshotRejectsDuplicateCellsAndMarksMissingInputsPartial() throws Exception {
    UUID metric =
        create(
            "metric-definitions",
            body(
                "evidenceIds",
                List.of(evidence),
                "code",
                "TEST_" + UUID.randomUUID(),
                "label",
                "Revenue",
                "statement",
                "income",
                "periodType",
                "duration",
                "unitKind",
                "currency",
                "definition",
                "Reported revenue",
                "signConvention",
                "source"));
    var cell =
        body(
            "metricId",
            metric,
            "periodStart",
            "2024-04-01",
            "periodEnd",
            "2025-03-31",
            "scope",
            "unspecified",
            "unit",
            "USD",
            "dimensions",
            List.of(),
            "missingReason",
            "not_collected");
    var members = List.of(member(company, "identity"), member(metric, "metric"));
    var response =
        request(
            "POST",
            "/api/v1/companies/" + company + "/snapshots",
            snapshotFixture(members, List.of(cell)));
    assertThat(response.statusCode()).isEqualTo(201);
    assertThat(json(response).at("/attributes/status").asText()).isEqualTo("partial");
    response =
        request(
            "POST",
            "/api/v1/companies/" + company + "/snapshots",
            snapshotFixture(members, List.of(cell, cell)));
    assertThat(response.statusCode()).isEqualTo(400);
  }

  @Test
  void snapshotRejectsEvidenceAfterCutoff() throws Exception {
    var snapshot = snapshotFixture(List.of(member(company, "identity")), List.of());
    snapshot.put("cutoffAt", "2025-01-01T00:00:00Z");
    snapshot.put("knowledgeBasis", "collected_by_cutoff");
    assertThat(
            request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot).statusCode())
        .isEqualTo(400);
  }

  @Test
  void unknownServerOwnedFieldsAndWrongResourceTypesAreRejected() throws Exception {
    var profile =
        body(
            "evidenceIds",
            List.of(evidence),
            "companyId",
            company,
            "field",
            "description",
            "profileValue",
            "Test",
            "id",
            UUID.randomUUID());
    assertThat(post("profile-observations", profile).statusCode()).isEqualTo(400);
    assertThat(request("GET", "/api/v1/company-data/securities/" + company, null).statusCode())
        .isEqualTo(404);
    assertThat(request("GET", "/api/v1/company-data/not-a-resource", null).statusCode())
        .isEqualTo(404);
    assertThat(
            request("GET", "/api/v1/company-data/financial-observations?madeUp=x", null)
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void filingSnapshotRequiresFrozenMappingAndFinancialImportsAreIdempotent() throws Exception {
    var fact = financialFixture();
    var first = post("financial-observations", fact);
    assertThat(first.statusCode()).isEqualTo(201);
    UUID observation = UUID.fromString(json(first).get("id").asText());
    var duplicate = post("financial-observations", fact);
    assertThat(duplicate.statusCode()).isEqualTo(201);
    assertThat(json(duplicate).get("id").asText()).isEqualTo(observation.toString());
    fact.put("value", "2.5");
    assertThat(post("financial-observations", fact).statusCode()).isEqualTo(409);
    UUID metric = createMetric();
    UUID mapping =
        create(
            "concept-metric-mappings",
            body(
                "evidenceIds",
                List.of(evidence),
                "conceptId",
                fact.get("conceptId"),
                "metricId",
                metric,
                "mappingVersion",
                "1",
                "semanticNotes",
                "Reviewed earnings concept"));
    var selections =
        List.of(
            member(company, "identity"),
            member(metric, "metric"),
            member(mapping, "mapping"),
            member(observation, "financial"),
            member((UUID) fact.get("conceptId"), "concept"),
            member((UUID) fact.get("filingId"), "filing"));
    var cell =
        body(
            "metricId",
            metric,
            "periodStart",
            "2024-04-01",
            "periodEnd",
            "2025-03-31",
            "scope",
            "unspecified",
            "unit",
            "USD/shares",
            "dimensions",
            List.of(),
            "observationId",
            observation);
    var snapshot = snapshotFixture(selections, List.of(cell));
    var result = request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot);
    assertThat(result.statusCode()).withFailMessage(result.body()).isEqualTo(201);
    var reversed = new ArrayList<>(selections);
    java.util.Collections.reverse(reversed);
    snapshot.put("members", reversed);
    var reordered = request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot);
    assertThat(reordered.statusCode()).isEqualTo(201);
    assertThat(json(reordered).at("/attributes/manifestSha256"))
        .isEqualTo(json(result).at("/attributes/manifestSha256"));
    snapshot.put("mappingVersion", "unreviewed");
    assertThat(
            request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot).statusCode())
        .isEqualTo(400);
    cell.put("unit", "INR");
    assertThat(
            request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot).statusCode())
        .isEqualTo(400);
  }

  @Test
  void vendorSnapshotRequiresVerifiedFieldMapping() throws Exception {
    UUID metric = createMetric();
    UUID endpoint =
        create(
            "source-endpoints",
            body(
                "evidenceIds",
                List.of(),
                "providerId",
                provider,
                "code",
                "income",
                "contractVersion",
                "1",
                "authScheme",
                "api_key",
                "supportedDomains",
                List.of("financials"),
                "paginationKind",
                "none",
                "accessStatus",
                "available",
                "checkedAt",
                "2026-10-22T12:00:00Z"));
    UUID observation =
        create(
            "vendor-financial-observations",
            body(
                "evidenceIds",
                List.of(evidence),
                "companyId",
                company,
                "providerId",
                provider,
                "sourceEndpointId",
                endpoint,
                "sourceRecordId",
                "2025-FY",
                "sourceField",
                "eps",
                "metricId",
                metric,
                "periodStart",
                "2024-04-01",
                "periodEnd",
                "2025-03-31",
                "periodType",
                "duration",
                "unit",
                "USD/shares",
                "value",
                "1.25",
                "scale",
                0,
                "reportedValueRaw",
                "1.25",
                "scope",
                "unspecified",
                "methodologyStatus",
                "reported_by_vendor",
                "mappingVersion",
                "1",
                "assertionKey",
                UUID.randomUUID().toString()));
    var cell =
        body(
            "metricId",
            metric,
            "periodStart",
            "2024-04-01",
            "periodEnd",
            "2025-03-31",
            "scope",
            "unspecified",
            "unit",
            "USD/shares",
            "dimensions",
            List.of(),
            "observationId",
            observation);
    var selected =
        new ArrayList<>(
            List.of(
                member(company, "identity"),
                member(metric, "metric"),
                member(observation, "financial")));
    var snapshot = snapshotFixture(selected, List.of(cell));
    assertThat(
            request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot).statusCode())
        .isEqualTo(400);
    UUID mapping =
        create(
            "vendor-metric-mappings",
            body(
                "evidenceIds",
                List.of(evidence),
                "providerId",
                provider,
                "sourceEndpointId",
                endpoint,
                "sourceField",
                "eps",
                "metricId",
                metric,
                "mappingVersion",
                "1",
                "definitionStatus",
                "verified",
                "semanticNotes",
                "Reviewed vendor EPS basis"));
    selected.add(member(mapping, "mapping"));
    var response = request("POST", "/api/v1/companies/" + company + "/snapshots", snapshot);
    assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(201);
  }

  @Test
  void crossIssuerListingsCannotBeAttachedToAnnouncements() throws Exception {
    String key = UUID.randomUUID().toString();
    var other =
        request(
            "POST",
            "/api/v1/companies",
            body(
                "company",
                body(
                    "evidenceIds",
                    List.of(evidence),
                    "slug",
                    "other-" + key,
                    "displayName",
                    "Other Issuer",
                    "entityKind",
                    "issuer"),
                "identifiers",
                List.of(
                    body(
                        "evidenceIds",
                        List.of(evidence),
                        "scheme",
                        "provider_company_id",
                        "authority",
                        "fixture",
                        "value",
                        key))));
    assertThat(other.statusCode()).isEqualTo(201);
    UUID otherCompany = UUID.fromString(json(other).get("id").asText());
    UUID security =
        create(
            "securities",
            body(
                "evidenceIds",
                List.of(evidence),
                "companyId",
                otherCompany,
                "securityType",
                "ordinary_share"));
    UUID listing =
        create(
            "listings",
            body(
                "evidenceIds",
                List.of(evidence),
                "securityId",
                security,
                "exchangeCode",
                "NSE",
                "symbol",
                key,
                "status",
                "active"));
    assertThat(
            post(
                    "announcements",
                    body(
                        "evidenceIds",
                        List.of(evidence),
                        "companyId",
                        company,
                        "listingId",
                        listing,
                        "providerId",
                        provider,
                        "sourceRecordId",
                        key,
                        "title",
                        "Wrong issuer",
                        "publishedAtRaw",
                        "raw"))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            request("GET", "/api/v1/company-data/listings?companyId=" + otherCompany, null)
                .statusCode())
        .isEqualTo(200);
  }
}

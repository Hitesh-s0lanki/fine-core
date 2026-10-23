package com.fincore.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class CompanyApiTestSupport extends PostgresTestSupport {
  @LocalServerPort int port;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  UUID evidence;
  UUID company;
  UUID provider;

  @BeforeEach
  void companyFixture() throws Exception {
    evidence =
        create(
            "source-references",
            body(
                "evidenceIds",
                List.of(),
                "extractionMethod",
                "configured",
                "observedAt",
                "2026-10-22T12:00:00Z"));
    String key = UUID.randomUUID().toString().replace("-", "");
    provider =
        create(
            "providers",
            body(
                "evidenceIds",
                List.of(),
                "code",
                "sec-" + key,
                "name",
                "SEC test source",
                "kind",
                "regulator"));
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
                    "issuer-" + key,
                    "displayName",
                    "Issuer " + key,
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
    assertThat(response.statusCode()).isEqualTo(201);
    company = UUID.fromString(json(response).get("id").asText());
  }

  UUID createMetric() throws Exception {
    return create(
        "metric-definitions",
        body(
            "evidenceIds",
            List.of(evidence),
            "code",
            "EPS_" + UUID.randomUUID(),
            "label",
            "EPS",
            "statement",
            "per_share",
            "periodType",
            "duration",
            "unitKind",
            "per_share",
            "definition",
            "Reported earnings per share",
            "signConvention",
            "source"));
  }

  Map<String, Object> financialFixture() throws Exception {
    UUID concept =
        create(
            "financial-concepts",
            body(
                "evidenceIds",
                List.of(evidence),
                "taxonomy",
                "ifrs-full",
                "conceptName",
                "Earnings" + UUID.randomUUID(),
                "periodType",
                "duration"));
    UUID filing =
        create(
            "filings",
            body(
                "evidenceIds",
                List.of(evidence),
                "companyId",
                company,
                "providerId",
                provider,
                "sourceFilingId",
                UUID.randomUUID().toString(),
                "formType",
                "20-F",
                "xbrlStatus",
                "yes"));
    return body(
        "evidenceIds",
        List.of(evidence),
        "companyId",
        company,
        "conceptId",
        concept,
        "filingId",
        filing,
        "periodType",
        "duration",
        "periodStart",
        "2024-04-01",
        "periodEnd",
        "2025-03-31",
        "scope",
        "unspecified",
        "dimensions",
        List.of(),
        "dimensionsStatus",
        "not_reported",
        "value",
        "0.123456789012345678",
        "unit",
        "USD/shares",
        "scale",
        0,
        "reportedValueRaw",
        "0.123456789012345678",
        "assertionKey",
        UUID.randomUUID().toString());
  }

  Map<String, Object> snapshotFixture(List<?> members, List<?> cells) {
    return body(
        "cutoffAt",
        "2026-10-23T00:00:00Z",
        "knowledgeBasis",
        "current_vintage",
        "selectionPolicyVersion",
        "explicit-v1",
        "mappingVersion",
        "1",
        "members",
        members,
        "financialCells",
        cells);
  }

  Map<String, Object> member(UUID id, String role) {
    return body("recordId", id, "role", role, "selectionReason", "Reviewed fixture selection");
  }

  UUID create(String resource, Map<String, Object> input) throws Exception {
    var response = post(resource, input);
    assertThat(response.statusCode())
        .withFailMessage("%s: %s", resource, response.body())
        .isEqualTo(201);
    return UUID.fromString(json(response).get("id").asText());
  }

  HttpResponse<String> post(String resource, Map<String, Object> input) throws Exception {
    return request("POST", "/api/v1/company-data/" + resource, input);
  }

  HttpResponse<String> request(String method, String path, Object input) throws Exception {
    try (HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
      var builder =
          HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
              .timeout(Duration.ofSeconds(30));
      if (input == null) {
        builder.GET();
      } else {
        builder
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(input)));
      }
      return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
  }

  JsonNode json(HttpResponse<String> response) {
    return mapper.readTree(response.body());
  }

  Map<String, Object> body(Object... pairs) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      result.put(pairs[i].toString(), pairs[i + 1]);
    }
    return result;
  }
}

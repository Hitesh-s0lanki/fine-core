package com.fincore.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fincore.server.service.SecDirectoryClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class CompanyDiscoveryIT extends CompanyApiTestSupport {
  @MockitoBean SecDirectoryClient directory;
  private String cik;
  private String ticker;

  @BeforeEach
  void directoryFixture() {
    cik =
        Long.toString(
            8000000000L + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 1000000000L));
    ticker = "FC" + cik;
    when(directory.directory())
        .thenReturn(
            new SecDirectoryClient.Directory(
                List.of(new SecDirectoryClient.Entry(cik, ticker, "FinCore Test Company")),
                Instant.parse("2026-11-16T12:00:00Z"),
                "file:///retained/test-directory.json",
                "a".repeat(64),
                100));
  }

  @Test
  void discoversWithoutWritingAndImportsEvidencedIdentityWithSearchableTicker() throws Exception {
    var found = request("GET", "/api/v1/companies/discovery/sec?query=" + ticker, null);
    assertThat(found.statusCode()).isEqualTo(200);
    assertThat(json(found).at("/items/0/cik").asText()).isEqualTo(cik);
    assertThat(json(found).at("/items/0/companyId").isNull()).isTrue();
    var imported = request("POST", "/api/v1/companies/discovery/sec/import", body("cik", cik));
    assertThat(imported.statusCode()).isEqualTo(201);
    var id = json(imported).at("/company/id").asText();
    assertThat(json(imported).get("coverage").asText()).isEqualTo("identity_only");
    assertThat(json(imported).at("/company/evidenceIds").size()).isEqualTo(1);
    var search = request("GET", "/api/v1/companies?query=" + ticker, null);
    assertThat(json(search).get("items").toString()).contains(id);
    UUID evidenceId = UUID.fromString(json(imported).at("/company/evidenceIds/0").asText());
    var retained =
        jdbc.queryForMap(
            "select s.source_url, s.locator, a.storage_uri, a.sha256 "
                + "from source_reference s join raw_artifact a on a.id=s.artifact_id where s.id=?",
            evidenceId);
    assertThat(retained.get("source_url")).isEqualTo(SecDirectoryClient.SOURCE_URL);
    assertThat(retained.get("locator").toString()).contains(cik);
    assertThat(retained.get("storage_uri")).isEqualTo("file:///retained/test-directory.json");
    assertThat(retained.get("sha256")).isEqualTo("a".repeat(64));
    var replay = request("POST", "/api/v1/companies/discovery/sec/import", body("cik", cik));
    assertThat(replay.statusCode()).isEqualTo(200);
    assertThat(json(replay).at("/company/id").asText()).isEqualTo(id);
    assertThat(json(replay).get("created").asBoolean()).isFalse();
    var known = request("GET", "/api/v1/companies/discovery/sec?query=" + cik, null);
    assertThat(json(known).at("/items/0/companyId").asText()).isEqualTo(id);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from company_identifier where scheme='cik' " + "and value=?",
                Long.class,
                cik))
        .isEqualTo(1);
  }

  @Test
  void reusesPreviouslyOnboardedCikRegardlessOfAuthorityCase() throws Exception {
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
    var result = request("POST", "/api/v1/companies/discovery/sec/import", body("cik", cik));
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(json(result).at("/company/id").asText()).isEqualTo(company.toString());
  }

  @Test
  void simultaneousImportsCreateOneCompany() {
    var first = CompletableFuture.supplyAsync(this::importStatus);
    var second = CompletableFuture.supplyAsync(this::importStatus);
    assertThat(List.of(first.join(), second.join())).containsExactlyInAnyOrder(201, 200);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from company_identifier where scheme='cik' " + "and value=?",
                Long.class,
                cik))
        .isEqualTo(1);
  }

  @Test
  void rejectsInvalidInputsAndUnknownDirectoryCik() throws Exception {
    for (var input :
        List.of(body(), body("cik", null), body("cik", "320193"), body("cik", "0000000000"))) {
      assertThat(request("POST", "/api/v1/companies/discovery/sec/import", input).statusCode())
          .isEqualTo(400);
    }
    assertThat(
            request("POST", "/api/v1/companies/discovery/sec/import", body("cik", "0000000001"))
                .statusCode())
        .isEqualTo(404);
    assertThat(request("GET", "/api/v1/companies/discovery/sec?query=&page=0", null).statusCode())
        .isEqualTo(400);
    assertThat(
            request("GET", "/api/v1/companies/discovery/sec?query=FCTEST&size=101", null)
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void multipleTickerAssociationsRemainSearchableWithoutCreatingDuplicateCompany()
      throws Exception {
    when(directory.directory())
        .thenReturn(
            new SecDirectoryClient.Directory(
                List.of(
                    new SecDirectoryClient.Entry(cik, ticker, "FinCore Test Company"),
                    new SecDirectoryClient.Entry(cik, ticker + "B", "FinCore Test Company")),
                Instant.parse("2026-11-16T12:00:00Z"),
                "file:///retained/test-directory.json",
                "b".repeat(64),
                100));
    var imported = request("POST", "/api/v1/companies/discovery/sec/import", body("cik", cik));
    assertThat(imported.statusCode()).isEqualTo(201);
    String id = json(imported).at("/company/id").asText();
    for (String alias : List.of(ticker, ticker + "B")) {
      var found = request("GET", "/api/v1/companies?query=" + alias, null);
      assertThat(json(found).get("items").toString()).contains(id);
    }
  }

  @Test
  void ambiguousStoredCikReturnsConflictInsteadOfSelectingAnArbitraryCompany() throws Exception {
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
    var created =
        request(
            "POST",
            "/api/v1/companies",
            body(
                "company",
                body(
                    "evidenceIds",
                    List.of(evidence),
                    "slug",
                    "ambiguous-" + cik,
                    "displayName",
                    "Ambiguous Test",
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
                        "SEC",
                        "value",
                        cik))));
    assertThat(created.statusCode()).isEqualTo(201);
    assertThat(
            request("POST", "/api/v1/companies/discovery/sec/import", body("cik", cik))
                .statusCode())
        .isEqualTo(409);
  }

  @Test
  void differentCompaniesReuseOneRetainedDirectoryArtifact() throws Exception {
    String secondCik = Long.toString(Long.parseLong(cik) + 1);
    String hash = UUID.randomUUID().toString().replace("-", "").repeat(2);
    when(directory.directory())
        .thenReturn(
            new SecDirectoryClient.Directory(
                List.of(
                    new SecDirectoryClient.Entry(cik, ticker, "First Issuer"),
                    new SecDirectoryClient.Entry(secondCik, ticker + "B", "Second Issuer")),
                Instant.parse("2026-11-16T12:00:00Z"),
                "file:///retained/" + hash + ".json",
                hash,
                100));
    for (String selected : List.of(cik, secondCik)) {
      assertThat(
              request("POST", "/api/v1/companies/discovery/sec/import", body("cik", selected))
                  .statusCode())
          .isEqualTo(201);
    }
    assertThat(
            jdbc.queryForObject(
                "select count(*) from raw_artifact where sha256=?", Long.class, hash))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from source_reference s join raw_artifact a "
                    + "on a.id=s.artifact_id where a.sha256=?",
                Long.class,
                hash))
        .isEqualTo(2);
  }

  private int importStatus() {
    try {
      return request("POST", "/api/v1/companies/discovery/sec/import", body("cik", cik))
          .statusCode();
    } catch (Exception error) {
      throw new IllegalStateException(error);
    }
  }
}

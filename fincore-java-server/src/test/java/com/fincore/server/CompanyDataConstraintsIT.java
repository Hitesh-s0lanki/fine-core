package com.fincore.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CompanyDataConstraintsIT extends CompanyApiTestSupport {
  @Autowired private PlatformTransactionManager transactions;

  @Test
  void malformedNestedProfilesReturnValidationProblemsInsteadOfServerErrors() throws Exception {
    for (var profile :
        List.of(
            body("field", "employee_count", "profileValue", body("count", 5)),
            body("field", "address", "profileValue", body("rawText", "Missing kind")))) {
      profile.put("evidenceIds", List.of(evidence));
      profile.put("companyId", company);
      var response = post("profile-observations", profile);
      assertThat(response.statusCode()).isEqualTo(400);
      assertThat(json(response).get("detail").asText()).contains("Invalid company data request");
    }
  }

  @Test
  void nullInitialIdentifierIsRejectedBeforeCompanyCreation() throws Exception {
    String slug = "null-identifier-" + UUID.randomUUID();
    var input =
        body(
            "company",
            body(
                "evidenceIds",
                List.of(evidence),
                "slug",
                slug,
                "displayName",
                "Invalid nested request",
                "entityKind",
                "issuer"),
            "identifiers",
            Collections.singletonList(null));
    assertThat(request("POST", "/api/v1/companies", input).statusCode()).isEqualTo(400);
    assertThat(
            jdbc.queryForObject("select count(*) from company where slug=?", Integer.class, slug))
        .isZero();
  }

  @ParameterizedTest
  @ValueSource(strings = {"members", "financialCells"})
  void nullSnapshotSelectionsAreRejectedWithoutSavingSnapshots(String section) throws Exception {
    var input = snapshotFixture(List.of(member(company, "identity")), List.of());
    input.put(section, Collections.singletonList(null));
    assertThat(request("POST", "/api/v1/companies/" + company + "/snapshots", input).statusCode())
        .isEqualTo(400);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from company_snapshot where company_id=?", Integer.class, company))
        .isZero();
  }

  @Test
  void companyScopedPaginationHasNoDuplicatesAcrossPages() throws Exception {
    var ids = new HashSet<String>();
    for (int i = 0; i < 3; i++) {
      ids.add(
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
                      "Description version " + i))
              .toString());
    }
    String path =
        "/api/v1/company-data/profile-observations?companyId=" + company + "&size=2&page=";
    var first = request("GET", path + "0", null);
    var second = request("GET", path + "1", null);
    assertThat(first.statusCode()).isEqualTo(200);
    assertThat(second.statusCode()).isEqualTo(200);
    assertThat(json(first).get("totalItems").asInt()).isEqualTo(3);
    assertThat(json(first).get("hasNext").asBoolean()).isTrue();
    assertThat(json(second).get("hasNext").asBoolean()).isFalse();
    var returned = new ArrayList<String>();
    json(first).get("items").forEach(item -> returned.add(item.get("id").asText()));
    json(second).get("items").forEach(item -> returned.add(item.get("id").asText()));
    assertThat(returned).hasSize(3).containsExactlyInAnyOrderElementsOf(ids);
  }

  @Test
  void financialPrecisionSurvivesSeparateReadAndPeriodFilter() throws Exception {
    var input = financialFixture();
    input.put("value", "123456789012345678901234567890123456789012.123456789012345678");
    UUID id = create("financial-observations", input);
    var result = request("GET", "/api/v1/company-data/financial-observations/" + id, null);
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(json(result).at("/attributes/value").asText()).isEqualTo(input.get("value"));
    var page =
        request(
            "GET",
            "/api/v1/company-data/financial-observations?companyId="
                + company
                + "&periodEnd=2025-03-31",
            null);
    assertThat(page.statusCode()).isEqualTo(200);
    assertThat(json(page).get("totalItems").asInt()).isEqualTo(1);
    assertThat(json(page).get("items").get(0).get("id").asText()).isEqualTo(id.toString());
  }

  @Test
  void ordinarySharesAndAdrsHaveSeparateListingPricesAndCurrencies() throws Exception {
    UUID ordinary = security("ordinary_share");
    UUID adr = security("adr");
    UUID india = listing(ordinary, "NSE", "INR");
    UUID usa = listing(adr, "NYSE", "USD");
    UUID inQuote =
        create(
            "market-quotes",
            body(
                "evidenceIds",
                List.of(evidence),
                "listingId",
                india,
                "asOf",
                "2026-10-22T12:00:00Z",
                "currency",
                "INR",
                "lastPrice",
                "1500.25"));
    UUID usQuote =
        create(
            "market-quotes",
            body(
                "evidenceIds",
                List.of(evidence),
                "listingId",
                usa,
                "asOf",
                "2026-10-22T12:00:00Z",
                "currency",
                "USD",
                "lastPrice",
                "18.50"));
    var indian =
        json(request("GET", "/api/v1/company-data/market-quotes?listingId=" + india, null));
    var american =
        json(request("GET", "/api/v1/company-data/market-quotes?listingId=" + usa, null));
    assertThat(indian.at("/items/0/id").asText()).isEqualTo(inQuote.toString());
    assertThat(american.at("/items/0/id").asText()).isEqualTo(usQuote.toString());
    assertThat(indian.at("/items/0/attributes/currency").asText()).isEqualTo("INR");
    assertThat(american.at("/items/0/attributes/currency").asText()).isEqualTo("USD");
  }

  @ParameterizedTest
  @CsvSource({"10,9,8,10", "10,12,8,13"})
  void invalidOhlcBoundsAreRejectedByPostgresAndReturnSanitizedConflict(
      String open, String high, String low, String close) throws Exception {
    UUID listing = listing(security("ordinary_share"), "NSE", "INR");
    var response =
        post(
            "price-bars",
            body(
                "evidenceIds",
                List.of(evidence),
                "listingId",
                listing,
                "interval",
                "daily",
                "tradingDate",
                "2026-10-22",
                "exchangeTimezone",
                "Asia/Kolkata",
                "currency",
                "INR",
                "open",
                open,
                "high",
                high,
                "low",
                low,
                "close",
                close,
                "adjustmentBasis",
                "raw"));
    assertThat(response.statusCode()).isEqualTo(409);
    assertThat(json(response).get("detail").asText())
        .doesNotContain("INSERT", "price_bar", "jdbc:");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from price_bar where listing_id=?", Integer.class, listing))
        .isZero();
  }

  @Test
  void foreignKeysProtectCompanyReferencesOutsideTheApi() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactions)
                    .executeWithoutResult(
                        status -> {
                          insertBase(id, "company-identifiers");
                          jdbc.update(
                              "insert into company_identifier(id,company_id,scheme,authorit"
                                  + "y,value) values (?,?,?,?,?)",
                              id,
                              UUID.randomUUID(),
                              "cik",
                              "sec",
                              "0001067491");
                        }))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject("select count(*) from data_record where id=?", Integer.class, id))
        .isZero();
  }

  @Test
  void databaseRejectsNonFiniteDecimalsEvenWhenApiValidationIsBypassed() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactions)
                    .executeWithoutResult(
                        status -> {
                          insertBase(id, "fx-observations");
                          jdbc.update(
                              "insert into fx_observation(id,base_currency,quote_currency,o"
                                  + "bserved_at,rate,rate_kind)"
                                  + " values (?,?,?,CURRENT_TIMESTAMP,'NaN'::numeric,?)",
                              id,
                              "USD",
                              "INR",
                              "spot");
                        }))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject("select count(*) from data_record where id=?", Integer.class, id))
        .isZero();
  }

  @Test
  void databaseRejectsMembershipAddedAfterSnapshotCommit() throws Exception {
    var response =
        request(
            "POST",
            "/api/v1/companies/" + company + "/snapshots",
            snapshotFixture(List.of(member(company, "identity")), List.of()));
    assertThat(response.statusCode()).isEqualTo(201);
    UUID snapshot = UUID.fromString(json(response).get("id").asText());
    UUID newMember = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transactions)
                    .executeWithoutResult(
                        status -> {
                          insertBase(newMember, "snapshot-members");
                          jdbc.update(
                              "insert into snapshot_member(id,snapshot_id,record_id,role,se"
                                  + "lection_reason) values (?,?,?,?,?)",
                              newMember,
                              snapshot,
                              company,
                              "extra",
                              "Attempt to change frozen manifest");
                        }))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from snapshot_member where snapshot_id=?",
                Integer.class,
                snapshot))
        .isEqualTo(1);
  }

  @Test
  void publishedCutoffCannotFallBackToAnUnknownPublicationDate() throws Exception {
    var input = snapshotFixture(List.of(member(company, "identity")), List.of());
    input.put("knowledgeBasis", "published_by_cutoff");
    var response = request("POST", "/api/v1/companies/" + company + "/snapshots", input);
    assertThat(response.statusCode()).isEqualTo(400);
  }

  private UUID security(String type) throws Exception {
    return create(
        "securities",
        body("evidenceIds", List.of(evidence), "companyId", company, "securityType", type));
  }

  private UUID listing(UUID security, String exchange, String currency) throws Exception {
    return create(
        "listings",
        body(
            "evidenceIds",
            List.of(evidence),
            "securityId",
            security,
            "exchangeCode",
            exchange,
            "symbol",
            "TEST" + UUID.randomUUID(),
            "tradingCurrency",
            currency,
            "status",
            "active"));
  }

  private void insertBase(UUID id, String type) {
    jdbc.update(
        "insert into data_record(id,record_type,schema_version,create"
            + "d_at) values (?,?,1,CURRENT_TIMESTAMP)",
        id,
        type);
  }
}

package com.fincore.server.service;

import static com.fincore.server.support.TestRecords.identified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fincore.server.dto.company.CompanyRequest;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.validation.RecordRules;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class CompanyDataServiceTest {
  private CompanyDataService service;
  private StaticApplicationContext context;
  private ValidatorFactory validators;
  private RecordRules rules;

  @BeforeEach
  void setUp() {
    context = new StaticApplicationContext();
    validators = Validation.buildDefaultValidatorFactory();
    rules = mock(RecordRules.class);
    service =
        new CompanyDataService(
            context, JsonMapper.builder().build(), validators.getValidator(), rules);
  }

  @AfterEach
  void closeResources() {
    validators.close();
    context.close();
  }

  @ParameterizedTest
  @ValueSource(strings = {"companies", "snapshots", "snapshot-members", "snapshot-financial-cells"})
  void dedicatedWorkflowsCannotBeBypassedThroughGenericCreation(String route) {
    assertThatThrownBy(() -> service.create(route, JsonMapper.builder().build().createObjectNode()))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(rules);
  }

  @ParameterizedTest
  @CsvSource({"-1,25", "0,0", "0,101"})
  void invalidPageBoundsAreRejectedWithoutDatabaseAccess(int page, int size) {
    assertThatThrownBy(() -> service.list("companies", page, size, Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void filtersMustBeAllowedAndPresentOnTheSelectedResource() {
    assertThatThrownBy(() -> service.list("companies", 0, 25, Map.of("sql", "ignored")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                service.list("companies", 0, 25, Map.of("listingId", UUID.randomUUID().toString())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unknownResourcesReturnNotFound() {
    assertThatThrownBy(() -> service.resource("java.lang.Runtime"))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void beanValidationRunsBeforeSemanticValidationOrPersistence() {
    var request = new CompanyRequest(Set.of(), "", "", "issuer");
    assertThatThrownBy(() -> service.save(service.resource("companies"), request))
        .isInstanceOf(ConstraintViolationException.class);
    verifyNoInteractions(rules);
  }

  @Test
  void responseSeparatesAuditFieldsAndPreservesExactDecimalStrings() {
    UUID id = UUID.randomUUID();
    UUID evidence = UUID.randomUUID();
    FinancialObservation fact = identified(new FinancialObservation(), id);
    fact.setValue(new BigDecimal("0.123456789012345678"));
    fact.setEvidenceIds(Set.of(evidence));
    var result = service.view(fact);
    assertThat(result.id()).isEqualTo(id);
    assertThat(result.recordType()).isEqualTo("financial-observations");
    assertThat(result.evidenceIds()).containsExactly(evidence);
    assertThat(result.attributes())
        .containsEntry("value", "0.123456789012345678")
        .doesNotContainKeys(
            "id",
            "createdAt",
            "schemaVersion",
            "evidenceIds",
            "companyReference",
            "filingReference");
  }
}

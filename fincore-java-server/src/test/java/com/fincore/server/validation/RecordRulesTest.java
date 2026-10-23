package com.fincore.server.validation;

import static com.fincore.server.support.TestRecords.identified;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.company.CompanyProfileObservation;
import com.fincore.server.models.disclosure.Filing;
import com.fincore.server.models.financial.FinancialConcept;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.models.source.RawArtifact;
import com.fincore.server.models.source.SourceReference;
import com.fincore.server.repository.common.DomainRecordRepository;
import com.fincore.server.repository.snapshot.SnapshotMemberRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordRulesTest {
  @Mock private DomainRecordRepository records;
  @Mock private EntityManager entityManager;
  @Mock private SnapshotMemberRepository members;
  private RecordRules rules;
  private final UUID companyId = UUID.randomUUID();
  private final UUID evidenceId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    rules = new RecordRules(records, entityManager, members);
  }

  @Test
  void businessAssertionsRequireEvidence() {
    Company company = new Company();
    company.setSlug("infosys");
    assertThatThrownBy(() -> rules.validate(company))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("require source evidence");
    verifyNoInteractions(records, entityManager, members);
  }

  @ParameterizedTest
  @MethodSource("validProfiles")
  void acceptsSourcedProfileValues(String field, Object value) {
    var profile = profile(field, value);
    assertThatCode(() -> rules.validate(profile)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @MethodSource("invalidProfiles")
  void malformedProfileValuesProduceValidationErrors(String field, Object value) {
    var profile = profile(field, value);
    assertThatThrownBy(() -> rules.validate(profile)).isInstanceOf(IllegalArgumentException.class);
  }

  static Stream<Arguments> validProfiles() {
    return Stream.of(
        Arguments.of("description", "Software services"),
        Arguments.of("domicile_country", "IN"),
        Arguments.of("reporting_currency", "USD"),
        Arguments.of("fiscal_year_end", Map.of("month", 2, "day", 29)),
        Arguments.of("employee_count", Map.of("count", 0, "basis", "full_time")),
        Arguments.of("address", Map.of("kind", "business", "rawText", "Bangalore, India")),
        Arguments.of("executive_summary", Map.of("name", "Reported executive", "role", "CEO")),
        Arguments.of(
            "sector_classification",
            Map.of("scheme", "provider", "label", "Technology", "authority", "vendor")));
  }

  static Stream<Arguments> invalidProfiles() {
    return Stream.of(
        Arguments.of("description", " "),
        Arguments.of("description", Map.of("text", "wrong shape")),
        Arguments.of("domicile_country", "India"),
        Arguments.of("reporting_currency", "usd"),
        Arguments.of("employee_count", Map.of("count", -1, "basis", "total")),
        Arguments.of("employee_count", Map.of("count", 1.5, "basis", "total")),
        Arguments.of("employee_count", Map.of("count", 5)),
        Arguments.of("address", Map.of("rawText", "Missing address kind")),
        Arguments.of("address", Map.of("kind", "unsupported", "rawText", "Address")),
        Arguments.of("executive_summary", Map.of("name", "Executive")),
        Arguments.of("sector_classification", Map.of("scheme", "provider", "label", "Technology")),
        Arguments.of("fiscal_year_end", Map.of("month", 3, "day", 31, "year", 2025)));
  }

  @Test
  void reversedEffectiveDatesAreRejected() {
    var profile = profile("description", "Sourced business description");
    profile.setEffectiveFrom(LocalDate.of(2026, 1, 1));
    profile.setEffectiveTo(LocalDate.of(2025, 1, 1));
    assertThatThrownBy(() -> rules.validate(profile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Reversed date interval");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"short", "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"})
  void artifactsRejectMalformedHashesWithoutConnection(String hash) {
    RawArtifact artifact = new RawArtifact();
    artifact.setSha256(hash);
    assertThatThrownBy(() -> rules.validate(artifact))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("SHA256");
    verifyNoInteractions(entityManager);
  }

  @ParameterizedTest
  @MethodSource("invalidDimensions")
  void financialDimensionsRejectAmbiguousAxesAndMalformedMembers(Object dimensions) {
    var fact = financial();
    fact.setDimensions(dimensions);
    assertThatThrownBy(() -> rules.validate(fact)).isInstanceOf(IllegalArgumentException.class);
  }

  static Stream<Arguments> invalidDimensions() {
    return Stream.of(
        Arguments.of(Map.of("axis", "Region", "member", "India")),
        Arguments.of(
            List.of(
                Map.of("axis", "Region", "member", "India"),
                Map.of("axis", "Region", "member", "US"))),
        Arguments.of(List.of(Map.of("axis", "Region", "kind", "typed", "member", "wrong field"))),
        Arguments.of(List.of(Map.of("axis", "Region", "kind", "explicit", "member", ""))),
        Arguments.of(List.of(Map.of("axis", "Region", "member", "India", "invented", true))));
  }

  @Test
  void typedDimensionsAndDurationPeriodsArePreserved() {
    var fact = financial();
    fact.setDimensions(
        List.of(
            Map.of(
                "axis",
                "CustomAxis",
                "kind",
                "typed",
                "typedValueCanonicalXml",
                "<value>India</value>")));
    assertThatCode(() -> rules.validate(fact)).doesNotThrowAnyException();
  }

  @Test
  void durationFactsCannotOmitTheirStartDate() {
    var fact = financial();
    fact.setPeriodStart(null);
    assertThatThrownBy(() -> rules.validate(fact))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("period semantics");
  }

  @Test
  void conceptPeriodTypeMustMatchTheReportedFact() {
    var fact = financial();
    fact.setPeriodType("instant");
    fact.setPeriodStart(null);
    assertThatThrownBy(() -> rules.validate(fact))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("period type mismatch");
  }

  @Test
  void filingAndFactCannotBelongToDifferentCompanies() {
    var fact = financial();
    Filing filing = entityManager.find(Filing.class, fact.getFilingId());
    filing.setCompanyId(UUID.randomUUID());
    assertThatThrownBy(() -> rules.validate(fact))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("different companies");
  }

  @Test
  void unknownPolymorphicReferencesAreRejected() {
    UUID id = UUID.randomUUID();
    when(records.findResourceById(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> rules.require(id, DomainRecord.class))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(entityManager);
  }

  private CompanyProfileObservation profile(String field, Object value) {
    evidenceAndCompany();
    var profile = new CompanyProfileObservation();
    profile.setEvidenceIds(Set.of(evidenceId));
    profile.setCompanyId(companyId);
    profile.setField(field);
    profile.setProfileValue(value);
    return profile;
  }

  private void evidenceAndCompany() {
    when(entityManager.find(SourceReference.class, evidenceId)).thenReturn(new SourceReference());
    when(entityManager.find(Company.class, companyId))
        .thenReturn(identified(new Company(), companyId));
  }

  private FinancialObservation financial() {
    evidenceAndCompany();
    final UUID conceptId = UUID.randomUUID();
    final UUID filingId = UUID.randomUUID();
    var concept = new FinancialConcept();
    concept.setPeriodType("duration");
    when(entityManager.find(FinancialConcept.class, conceptId)).thenReturn(concept);
    var filing = new Filing();
    filing.setCompanyId(companyId);
    when(entityManager.find(Filing.class, filingId)).thenReturn(filing);
    var fact = new FinancialObservation();
    fact.setEvidenceIds(Set.of(evidenceId));
    fact.setCompanyId(companyId);
    fact.setConceptId(conceptId);
    fact.setFilingId(filingId);
    fact.setPeriodType("duration");
    fact.setPeriodStart(LocalDate.of(2024, 4, 1));
    fact.setPeriodEnd(LocalDate.of(2025, 3, 31));
    fact.setDimensions(List.of());
    return fact;
  }
}

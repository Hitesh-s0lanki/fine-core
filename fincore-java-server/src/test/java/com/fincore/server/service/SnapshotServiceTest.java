package com.fincore.server.service;

import static com.fincore.server.support.TestRecords.identified;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fincore.server.config.Resources;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.snapshot.CellSelection;
import com.fincore.server.dto.snapshot.CompanySnapshotRequest;
import com.fincore.server.dto.snapshot.MemberSelection;
import com.fincore.server.dto.snapshot.SnapshotRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.company.CompanyProfileObservation;
import com.fincore.server.models.financial.MetricDefinition;
import com.fincore.server.models.macro.MacroObservation;
import com.fincore.server.models.quality.CoverageItem;
import com.fincore.server.models.snapshot.CompanySnapshot;
import com.fincore.server.models.source.SourceReference;
import com.fincore.server.repository.snapshot.SnapshotFinancialCellRepository;
import com.fincore.server.repository.snapshot.SnapshotMemberRepository;
import com.fincore.server.validation.RecordRules;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {
  @Mock private CompanyDataServiceInter data;
  @Mock private RecordRules rules;
  @Mock private SnapshotMemberRepository members;
  @Mock private SnapshotFinancialCellRepository cells;
  private SnapshotService service;
  private final UUID companyId = UUID.randomUUID();
  private final List<CreationRequest> saved = new ArrayList<>();

  @BeforeEach
  void setUp() {
    service = new SnapshotService(data, rules, members, cells, JsonMapper.builder().build());
  }

  @Test
  void snapshotCannotContainAnotherIssuersAssertion() {
    final Company company = company();
    CompanyProfileObservation profile =
        identified(new CompanyProfileObservation(), UUID.randomUUID());
    when(rules.require(profile.getId(), DomainRecord.class)).thenReturn(profile);
    when(rules.companyOf(profile)).thenReturn(UUID.randomUUID());
    assertThatThrownBy(
            () ->
                service.create(
                    companyId,
                    request(List.of(selection(company), selection(profile)), "current_vintage")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("another company");
    verify(data, never()).save(any(), any());
  }

  @Test
  void duplicateMembersAreRejectedBeforeAnySnapshotIsSaved() {
    final Company company = company();
    assertThatThrownBy(
            () ->
                service.create(
                    companyId,
                    request(List.of(selection(company), selection(company)), "current_vintage")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Duplicate snapshot member");
    verify(data, never()).save(any(), any());
  }

  @Test
  void incompleteCoverageMakesTheManifestPartialEvenWithoutFinancialCells() {
    final Company company = company();
    CoverageItem coverage = identified(new CoverageItem(), UUID.randomUUID());
    coverage.setStatus("blocked");
    when(rules.require(coverage.getId(), DomainRecord.class)).thenReturn(coverage);
    persistence();
    service.create(
        companyId, request(List.of(selection(company), selection(coverage)), "current_vintage"));
    assertThat(snapshotRequest().status()).isEqualTo("partial");
    assertThat(snapshotRequest().manifestSha256()).matches("[a-f0-9]{64}");
  }

  @Test
  void manifestHashesIgnoreMemberOrderButIncludeSelectionReasons() {
    final Company company = company();
    CompanyProfileObservation profile =
        identified(new CompanyProfileObservation(), UUID.randomUUID());
    when(rules.require(profile.getId(), DomainRecord.class)).thenReturn(profile);
    persistence();
    service.create(
        companyId, request(List.of(selection(company), selection(profile)), "current_vintage"));
    String first = snapshotRequest().manifestSha256();
    saved.clear();
    service.create(
        companyId, request(List.of(selection(profile), selection(company)), "current_vintage"));
    assertThat(snapshotRequest().manifestSha256()).isEqualTo(first);
    saved.clear();
    service.create(
        companyId,
        request(
            List.of(
                selection(company),
                new MemberSelection(profile.getId(), "data", "Different rationale")),
            "current_vintage"));
    assertThat(snapshotRequest().manifestSha256()).isNotEqualTo(first);
  }

  @Test
  void collectionAfterCutoffCannotEnterHistoricalSnapshots() {
    final Company company = company();
    UUID evidenceId = UUID.randomUUID();
    company.setEvidenceIds(Set.of(evidenceId));
    SourceReference evidence = new SourceReference();
    evidence.setObservedAt(Instant.parse("2026-10-24T00:00:00Z"));
    when(rules.require(evidenceId, SourceReference.class)).thenReturn(evidence);
    assertThatThrownBy(
            () ->
                service.create(
                    companyId, request(List.of(selection(company)), "collected_by_cutoff")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cutoff");
    verify(data, never()).save(any(), any());
  }

  @Test
  void dateOnlyPublicationCannotEstablishIntradayAvailability() {
    final Company company = company();
    UUID evidenceId = UUID.randomUUID();
    company.setEvidenceIds(Set.of(evidenceId));
    SourceReference evidence = new SourceReference();
    evidence.setAvailableAt(Instant.parse("2026-10-22T00:00:00Z"));
    evidence.setAvailabilityPrecision("date");
    when(rules.require(evidenceId, SourceReference.class)).thenReturn(evidence);
    assertThatThrownBy(
            () ->
                service.create(
                    companyId, request(List.of(selection(company)), "published_by_cutoff")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("intraday");
  }

  @Test
  void currentDownloadMacroDataCannotBeRecastAsHistoricalEvidence() {
    final Company company = company();
    MacroObservation macro = identified(new MacroObservation(), UUID.randomUUID());
    macro.setVintageBasis("current_download");
    when(rules.require(macro.getId(), DomainRecord.class)).thenReturn(macro);
    assertThatThrownBy(
            () ->
                service.create(
                    companyId,
                    request(List.of(selection(company), selection(macro)), "collected_by_cutoff")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("point-in-time");
  }

  @ParameterizedTest
  @CsvSource({"unknown,false", "duration,false", "instant,true"})
  void unknownOrIncompatibleMetricPeriodsBlockFinancialCells(String periodType, boolean hasStart) {
    final Company company = company();
    UUID metricId = UUID.randomUUID();
    MetricDefinition metric = identified(new MetricDefinition(), metricId);
    metric.setPeriodType(periodType);
    when(rules.require(metricId, DomainRecord.class)).thenReturn(metric);
    when(rules.require(metricId, MetricDefinition.class)).thenReturn(metric);
    var base = request(List.of(selection(company), selection(metric)), "current_vintage");
    var cell =
        new CellSelection(
            metricId,
            hasStart ? LocalDate.of(2024, 4, 1) : null,
            LocalDate.of(2025, 3, 31),
            "unspecified",
            "USD",
            null,
            List.of(),
            null,
            "Missing");
    var request =
        new SnapshotRequest(
            base.cutoffAt(),
            base.knowledgeBasis(),
            base.selectionPolicyVersion(),
            base.mappingVersion(),
            base.members(),
            List.of(cell));
    assertThatThrownBy(() -> service.create(companyId, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("period semantics");
    verify(data, never()).save(any(), any());
  }

  @Test
  void overviewRejectsAnotherCompanysSnapshotWithoutReadingItsMembers() {
    UUID snapshotId = UUID.randomUUID();
    CompanySnapshot snapshot = new CompanySnapshot();
    snapshot.setCompanyId(UUID.randomUUID());
    when(data.load("snapshots", snapshotId)).thenReturn(snapshot);
    assertThatThrownBy(() -> service.overview(companyId, snapshotId))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    verifyNoInteractions(members, cells);
  }

  private Company company() {
    Company company = identified(new Company(), companyId);
    when(data.load("companies", companyId)).thenReturn(company);
    when(rules.require(companyId, DomainRecord.class)).thenReturn(company);
    when(rules.companyOf(company)).thenReturn(companyId);
    return company;
  }

  private SnapshotRequest request(List<MemberSelection> selected, String knowledgeBasis) {
    return new SnapshotRequest(
        Instant.parse("2026-10-23T12:00:00Z"),
        knowledgeBasis,
        "reviewed-v1",
        "1",
        selected,
        List.of());
  }

  private MemberSelection selection(DomainRecord record) {
    return new MemberSelection(record.getId(), "data", "Reviewed evidence");
  }

  private void persistence() {
    when(data.resource(anyString()))
        .thenAnswer(
            invocation ->
                Resources.ALL.stream()
                    .filter(r -> r.route().equals(invocation.getArgument(0)))
                    .findFirst()
                    .orElseThrow());
    when(data.save(any(), any()))
        .thenAnswer(
            invocation -> {
              CreationRequest request = invocation.getArgument(1);
              saved.add(request);
              return identified(request.toEntity(), UUID.randomUUID());
            });
    when(data.view(any()))
        .thenAnswer(
            invocation -> {
              DomainRecord record = invocation.getArgument(0);
              return new RecordView(
                  record.getId(), "snapshots", 1, record.getCreatedAt(), Set.of(), Map.of());
            });
  }

  private CompanySnapshotRequest snapshotRequest() {
    return saved.stream()
        .filter(CompanySnapshotRequest.class::isInstance)
        .map(CompanySnapshotRequest.class::cast)
        .findFirst()
        .orElseThrow();
  }
}

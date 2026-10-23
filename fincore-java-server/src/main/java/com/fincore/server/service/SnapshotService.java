package com.fincore.server.service;

import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.company.CompanyOverview;
import com.fincore.server.dto.snapshot.CellSelection;
import com.fincore.server.dto.snapshot.CompanySnapshotRequest;
import com.fincore.server.dto.snapshot.MemberSelection;
import com.fincore.server.dto.snapshot.SnapshotFinancialCellRequest;
import com.fincore.server.dto.snapshot.SnapshotMemberRequest;
import com.fincore.server.dto.snapshot.SnapshotRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.SnapshotServiceInter;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.ConceptMetricMapping;
import com.fincore.server.models.financial.DerivedMetricObservation;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.models.financial.MetricDefinition;
import com.fincore.server.models.financial.VendorFinancialObservation;
import com.fincore.server.models.financial.VendorMetricMapping;
import com.fincore.server.models.macro.MacroObservation;
import com.fincore.server.models.quality.CoverageItem;
import com.fincore.server.models.security.Security;
import com.fincore.server.models.snapshot.CompanySnapshot;
import com.fincore.server.models.snapshot.SnapshotFinancialCell;
import com.fincore.server.models.snapshot.SnapshotMember;
import com.fincore.server.models.source.IngestionAttempt;
import com.fincore.server.models.source.SourceReference;
import com.fincore.server.repository.snapshot.SnapshotFinancialCellRepository;
import com.fincore.server.repository.snapshot.SnapshotMemberRepository;
import com.fincore.server.validation.RecordRules;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Validates and freezes explicit analyst/adapter selections in an immutable company snapshot. */
@Service
@Transactional(readOnly = true)
public class SnapshotService implements SnapshotServiceInter {
  private final CompanyDataServiceInter data;
  private final RecordRules rules;
  private final SnapshotMemberRepository members;
  private final SnapshotFinancialCellRepository cells;
  private final ObjectMapper mapper;

  /** Uses transactional data creation, evidence validation and typed snapshot repositories. */
  public SnapshotService(
      CompanyDataServiceInter data,
      RecordRules rules,
      SnapshotMemberRepository members,
      SnapshotFinancialCellRepository cells,
      ObjectMapper mapper) {
    this.data = data;
    this.rules = rules;
    this.members = members;
    this.cells = cells;
    this.mapper = mapper;
  }

  /** Validates issuer ownership, cutoffs and selected cells, then commits a frozen manifest. */
  @Transactional
  public RecordView create(UUID companyId, SnapshotRequest request) {
    data.load("companies", companyId);
    Map<UUID, DomainRecord> selected = new TreeMap<>();
    Set<String> roles = new HashSet<>();
    for (MemberSelection member : request.members()) {
      DomainRecord record = rules.require(member.recordId(), DomainRecord.class);
      if (record instanceof CompanySnapshot
          || record instanceof SnapshotMember
          || record instanceof SnapshotFinancialCell) {
        throw new IllegalArgumentException("Snapshots cannot contain mutable or nested manifests");
      }
      if (!roles.add(member.recordId() + ":" + member.role())) {
        throw new IllegalArgumentException("Duplicate snapshot member");
      }
      UUID owner = rules.companyOf(record);
      if (owner != null && !companyId.equals(owner)) {
        throw new IllegalArgumentException("Snapshot member belongs to another company");
      }
      validateCutoff(record, request);
      selected.put(record.getId(), record);
    }
    if (!selected.containsKey(companyId)) {
      throw new IllegalArgumentException("Snapshot must include company identity");
    }
    Set<String> cellKeys = new HashSet<>();
    for (CellSelection cell : request.financialCells()) {
      validateCell(companyId, cell, selected, request.mappingVersion());
      if (!cellKeys.add(cellKey(cell))) {
        throw new IllegalArgumentException("Duplicate financial cell");
      }
    }
    String status =
        request.financialCells().stream().anyMatch(c -> c.observationId() == null)
                || selected.values().stream()
                    .anyMatch(
                        r ->
                            r instanceof CoverageItem coverage
                                && !Set.of("available", "not_applicable")
                                    .contains(coverage.getStatus()))
            ? "partial"
            : "complete";
    CompanySnapshot snapshot =
        (CompanySnapshot)
            data.save(
                data.resource("snapshots"),
                new CompanySnapshotRequest(
                    Set.of(),
                    companyId,
                    request.cutoffAt(),
                    request.knowledgeBasis(),
                    request.selectionPolicyVersion(),
                    request.mappingVersion(),
                    manifestHash(companyId, request),
                    status));
    for (MemberSelection member : request.members()) {
      data.save(
          data.resource("snapshot-members"),
          new SnapshotMemberRequest(
              Set.of(),
              snapshot.getId(),
              member.recordId(),
              member.role(),
              member.selectionReason()));
    }
    for (CellSelection cell : request.financialCells()) {
      DomainRecord observation =
          cell.observationId() == null ? null : selected.get(cell.observationId());
      data.save(
          data.resource("snapshot-financial-cells"),
          new SnapshotFinancialCellRequest(
              Set.of(),
              snapshot.getId(),
              cell.metricId(),
              cell.periodStart(),
              cell.periodEnd(),
              cell.scope(),
              cell.unit(),
              cell.securityId(),
              cell.dimensions(),
              observation instanceof FinancialObservation ? cell.observationId() : null,
              observation instanceof VendorFinancialObservation ? cell.observationId() : null,
              observation instanceof DerivedMetricObservation ? cell.observationId() : null,
              cell.missingReason(),
              cellKey(cell)));
    }
    return data.view(snapshot);
  }

  /** Composes only frozen members; it does not join current preferred company assertions. */
  public CompanyOverview overview(UUID companyId, UUID snapshotId) {
    CompanySnapshot snapshot = (CompanySnapshot) data.load("snapshots", snapshotId);
    if (!companyId.equals(snapshot.getCompanyId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Snapshot not found for company");
    }
    Map<String, List<RecordView>> sections = new TreeMap<>();
    for (SnapshotMember member : members.findBySnapshotIdOrderById(snapshotId)) {
      sections
          .computeIfAbsent(member.getRole(), ignored -> new ArrayList<>())
          .add(data.view(rules.require(member.getRecordId(), DomainRecord.class)));
    }
    return new CompanyOverview(
        companyId,
        snapshotId,
        snapshot.getCutoffAt(),
        data.view(snapshot),
        sections,
        cells.findBySnapshotIdOrderByCellKey(snapshotId).stream().map(data::view).toList());
  }

  private void validateCell(
      UUID companyId, CellSelection cell, Map<UUID, DomainRecord> selected, String mappingVersion) {
    if (cell.periodStart() != null && cell.periodStart().isAfter(cell.periodEnd())) {
      throw new IllegalArgumentException("Invalid selected period");
    }
    MetricDefinition metric =
        (MetricDefinition) rules.require(cell.metricId(), MetricDefinition.class);
    if ((metric.getPeriodType().equals("duration") && cell.periodStart() == null)
        || (metric.getPeriodType().equals("instant") && cell.periodStart() != null)
        || metric.getPeriodType().equals("unknown")) {
      throw new IllegalArgumentException(
          "Selected cell has unknown or incompatible period semantics");
    }
    if (!selected.containsKey(metric.getId())) {
      throw new IllegalArgumentException("Metric definition must be frozen");
    }
    if (cell.securityId() != null
        && !companyId.equals(rules.companyOf(rules.require(cell.securityId(), Security.class)))) {
      throw new IllegalArgumentException("Cell security belongs to another company");
    }
    if ((cell.observationId() == null)
        != (cell.missingReason() != null && !cell.missingReason().isBlank())) {
      throw new IllegalArgumentException("Choose an observation or a nonblank missing reason");
    }
    if (cell.observationId() == null) {
      return;
    }
    DomainRecord observation = selected.get(cell.observationId());
    if (!(observation instanceof FinancialObservation
        || observation instanceof VendorFinancialObservation
        || observation instanceof DerivedMetricObservation)) {
      throw new IllegalArgumentException("Cell observation must be a frozen financial record");
    }
    if (!companyId.equals(rules.companyOf(observation))) {
      throw new IllegalArgumentException("Wrong issuer");
    }
    var bean = PropertyAccessorFactory.forBeanPropertyAccess(observation);
    if (!Objects.equals(cell.periodStart(), bean.getPropertyValue("periodStart"))
        || !Objects.equals(cell.periodEnd(), bean.getPropertyValue("periodEnd"))
        || !Objects.equals(cell.scope(), bean.getPropertyValue("scope"))
        || !Objects.equals(cell.unit(), bean.getPropertyValue("unit"))
        || !Objects.equals(cell.securityId(), bean.getPropertyValue("securityId"))) {
      throw new IllegalArgumentException("Selected cell does not match observation semantics");
    }
    Object dimensions =
        bean.isReadableProperty("dimensions") ? bean.getPropertyValue("dimensions") : List.of();
    if (!canonicalDimensions(cell.dimensions()).equals(canonicalDimensions(dimensions))) {
      throw new IllegalArgumentException("Selected dimensions differ from the observation");
    }
    if (observation instanceof FinancialObservation fact) {
      boolean mapped =
          selected.values().stream()
              .anyMatch(
                  r ->
                      r instanceof ConceptMetricMapping mapping
                          && mapping.getConceptId().equals(fact.getConceptId())
                          && mapping.getMetricId().equals(cell.metricId())
                          && mapping.getMappingVersion().equals(mappingVersion)
                          && (mapping.getTransformMethod() == null
                              || mapping.getTransformMethod().equals("identity"))
                          && (mapping.getValidFrom() == null
                              || !mapping.getValidFrom().isAfter(cell.periodEnd()))
                          && (mapping.getValidTo() == null
                              || !mapping.getValidTo().isBefore(cell.periodEnd()))
                          && (mapping.getCompanyId() == null
                              || mapping.getCompanyId().equals(companyId))
                          && (mapping.getScopeConstraint() == null
                              || mapping.getScopeConstraint().equals(cell.scope())));
      if (!mapped) {
        throw new IllegalArgumentException("No frozen concept-to-metric mapping");
      }
    } else {
      if (!cell.metricId().equals(bean.getPropertyValue("metricId"))) {
        throw new IllegalArgumentException("Selected metric differs from observation");
      }
      if (observation instanceof VendorFinancialObservation vendor) {
        boolean mapped =
            selected.values().stream()
                .anyMatch(
                    r ->
                        r instanceof VendorMetricMapping mapping
                            && mapping.getMetricId().equals(cell.metricId())
                            && mapping.getProviderId().equals(vendor.getProviderId())
                            && mapping.getSourceEndpointId().equals(vendor.getSourceEndpointId())
                            && mapping.getSourceField().equals(vendor.getSourceField())
                            && mapping.getDefinitionStatus().equals("verified")
                            && mapping.getMappingVersion().equals(mappingVersion)
                            && mappingVersion.equals(vendor.getMappingVersion())
                            && (mapping.getTransformMethod() == null
                                || mapping.getTransformMethod().equals("identity")));
        if (!mapped) {
          throw new IllegalArgumentException("Vendor metric requires a frozen verified mapping");
        }
      }
    }
  }

  private void validateCutoff(DomainRecord record, SnapshotRequest request) {
    if (request.knowledgeBasis().equals("current_vintage")) {
      return;
    }
    for (UUID evidenceId : record.getEvidenceIds()) {
      SourceReference evidence = (SourceReference) rules.require(evidenceId, SourceReference.class);
      Instant available =
          request.knowledgeBasis().equals("collected_by_cutoff")
              ? evidence.getObservedAt()
              : evidence.getAvailableAt();
      if (available == null || available.isAfter(request.cutoffAt())) {
        throw new IllegalArgumentException("Evidence is unavailable at requested cutoff");
      }
      if (request.knowledgeBasis().equals("published_by_cutoff")
          && !"timestamp".equals(evidence.getAvailabilityPrecision())
          && !request
              .cutoffAt()
              .atOffset(ZoneOffset.UTC)
              .toLocalTime()
              .equals(java.time.LocalTime.MAX)) {
        throw new IllegalArgumentException(
            "Date-only availability cannot establish an intraday cutoff");
      }
      if (request.knowledgeBasis().equals("collected_by_cutoff")
          && evidence.getIngestionAttemptId() != null) {
        IngestionAttempt attempt =
            (IngestionAttempt)
                rules.require(evidence.getIngestionAttemptId(), IngestionAttempt.class);
        if (attempt.getRetrievedAt() == null
            || attempt.getRetrievedAt().isAfter(request.cutoffAt())) {
          throw new IllegalArgumentException("Source was not retrieved by the cutoff");
        }
      }
    }
    if (record instanceof MacroObservation macro
        && macro.getVintageBasis().equals("current_download")) {
      throw new IllegalArgumentException(
          "Current-vintage macro data is not point-in-time evidence");
    }
  }

  private String manifestHash(UUID companyId, SnapshotRequest request) {
    List<String> lines = new ArrayList<>();
    lines.add(
        companyId
            + "|"
            + request.cutoffAt()
            + "|"
            + request.knowledgeBasis()
            + "|"
            + request.mappingVersion()
            + "|"
            + request.selectionPolicyVersion());
    request.members().stream()
        .map(m -> mapper.writeValueAsString(List.of(m.recordId(), m.role(), m.selectionReason())))
        .sorted()
        .forEach(lines::add);
    request.financialCells().stream()
        .map(c -> cellKey(c) + "|" + c.observationId() + "|" + c.missingReason())
        .sorted()
        .forEach(lines::add);
    return hash(String.join("\n", lines));
  }

  private String cellKey(CellSelection cell) {
    return hash(
        mapper.writeValueAsString(
            Arrays.asList(
                cell.metricId(),
                cell.periodStart(),
                cell.periodEnd(),
                cell.scope(),
                cell.unit(),
                cell.securityId(),
                canonicalDimensions(cell.dimensions()))));
  }

  private String canonicalDimensions(Object value) {
    if (!(value instanceof List<?> dimensions)) {
      throw new IllegalArgumentException("Dimensions require an array");
    }
    List<String> values = new ArrayList<>();
    for (Object raw : dimensions) {
      if (!(raw instanceof Map<?, ?> map)) {
        throw new IllegalArgumentException("Dimension requires an object");
      }
      Map<String, Object> sorted = new TreeMap<>();
      map.forEach((key, item) -> sorted.put(key.toString(), item));
      values.add(mapper.writeValueAsString(sorted));
    }
    Collections.sort(values);
    return mapper.writeValueAsString(values);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}

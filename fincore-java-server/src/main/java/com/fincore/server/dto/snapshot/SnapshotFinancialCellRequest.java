package com.fincore.server.dto.snapshot;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.snapshot.SnapshotFinancialCell;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for snapshot financial cell. */
public record SnapshotFinancialCellRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID snapshotId,
    @NotNull UUID metricId,
    LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotNull @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @NotBlank @Size(max = 40000) String unit,
    UUID securityId,
    @NotNull Object dimensions,
    UUID financialObservationId,
    UUID vendorFinancialObservationId,
    UUID derivedMetricObservationId,
    @Size(max = 40000) String missingReason,
    @NotBlank @Size(max = 40000) String cellKey)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    SnapshotFinancialCell entity = new SnapshotFinancialCell();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSnapshotId(snapshotId);
    entity.setMetricId(metricId);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setScope(scope);
    entity.setUnit(unit);
    entity.setSecurityId(securityId);
    entity.setDimensions(dimensions);
    entity.setFinancialObservationId(financialObservationId);
    entity.setVendorFinancialObservationId(vendorFinancialObservationId);
    entity.setDerivedMetricObservationId(derivedMetricObservationId);
    entity.setMissingReason(missingReason);
    entity.setCellKey(cellKey);
    return entity;
  }
}

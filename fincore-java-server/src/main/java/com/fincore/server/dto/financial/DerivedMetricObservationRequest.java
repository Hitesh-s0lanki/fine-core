package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.DerivedMetricObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for derived metric observation. */
public record DerivedMetricObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull UUID metricId,
    @NotNull UUID snapshotId,
    UUID securityId,
    @NotNull Object dimensions,
    LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotNull @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
    @NotBlank @Size(max = 40000) String unit,
    @NotNull Integer scale,
    @NotNull Object inputRecordRefs,
    @NotBlank @Size(max = 40000) String method,
    @NotBlank @Size(max = 40000) String methodVersion,
    @NotNull Instant computedAt)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    DerivedMetricObservation entity = new DerivedMetricObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setMetricId(metricId);
    entity.setSnapshotId(snapshotId);
    entity.setSecurityId(securityId);
    entity.setDimensions(dimensions);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setScope(scope);
    entity.setValue(Decimals.parse(value));
    entity.setUnit(unit);
    entity.setScale(scale);
    entity.setInputRecordRefs(inputRecordRefs);
    entity.setMethod(method);
    entity.setMethodVersion(methodVersion);
    entity.setComputedAt(computedAt);
    return entity;
  }
}

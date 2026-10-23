package com.fincore.server.dto.quality;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.quality.CoverageItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for coverage item. */
public record CoverageItemRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    UUID companyId,
    UUID providerId,
    @NotBlank @Size(max = 40000) String domain,
    @Size(max = 40000) String endpoint,
    UUID metricId,
    LocalDate periodStart,
    LocalDate periodEnd,
    @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @Size(max = 40000) String unit,
    @NotNull Instant checkedAt,
    @NotNull @Pattern(regexp = "available|partial|missing|blocked|not_attempted|not_applicable")
        String status,
    @Size(max = 40000) String reasonCode,
    @Size(max = 40000) String message,
    @NotNull Object ingestionAttemptIds,
    LocalDate latestObservationDate,
    LocalDate expectedThrough,
    @Size(max = 40000) String freshnessPolicyVersion)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CoverageItem entity = new CoverageItem();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setProviderId(providerId);
    entity.setDomain(domain);
    entity.setEndpoint(endpoint);
    entity.setMetricId(metricId);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setScope(scope);
    entity.setUnit(unit);
    entity.setCheckedAt(checkedAt);
    entity.setStatus(status);
    entity.setReasonCode(reasonCode);
    entity.setMessage(message);
    entity.setIngestionAttemptIds(ingestionAttemptIds);
    entity.setLatestObservationDate(latestObservationDate);
    entity.setExpectedThrough(expectedThrough);
    entity.setFreshnessPolicyVersion(freshnessPolicyVersion);
    return entity;
  }
}

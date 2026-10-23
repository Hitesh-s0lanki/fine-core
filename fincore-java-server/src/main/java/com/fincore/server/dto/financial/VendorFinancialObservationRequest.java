package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.VendorFinancialObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for vendor financial observation. */
public record VendorFinancialObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    UUID securityId,
    UUID listingId,
    @NotNull UUID providerId,
    @NotNull UUID sourceEndpointId,
    @NotBlank @Size(max = 40000) String sourceRecordId,
    @NotBlank @Size(max = 40000) String sourceField,
    UUID metricId,
    LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotNull @Pattern(regexp = "instant|duration|unknown") String periodType,
    @Size(max = 40000) String periodLabelRaw,
    @Size(max = 40000) String reportedCurrency,
    @NotBlank @Size(max = 40000) String unit,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
    @NotNull Integer scale,
    @NotBlank @Size(max = 40000) String reportedValueRaw,
    @NotNull @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @Size(max = 40000) String sourceFilingId,
    UUID filingId,
    LocalDate filedOn,
    Instant acceptedAt,
    @NotNull @Pattern(regexp = "reported_by_vendor|vendor_calculated|unknown")
        String methodologyStatus,
    @Size(max = 40000) String mappingVersion,
    @NotBlank @Size(max = 40000) String assertionKey)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    VendorFinancialObservation entity = new VendorFinancialObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setSecurityId(securityId);
    entity.setListingId(listingId);
    entity.setProviderId(providerId);
    entity.setSourceEndpointId(sourceEndpointId);
    entity.setSourceRecordId(sourceRecordId);
    entity.setSourceField(sourceField);
    entity.setMetricId(metricId);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setPeriodType(periodType);
    entity.setPeriodLabelRaw(periodLabelRaw);
    entity.setReportedCurrency(reportedCurrency);
    entity.setUnit(unit);
    entity.setValue(Decimals.parse(value));
    entity.setScale(scale);
    entity.setReportedValueRaw(reportedValueRaw);
    entity.setScope(scope);
    entity.setSourceFilingId(sourceFilingId);
    entity.setFilingId(filingId);
    entity.setFiledOn(filedOn);
    entity.setAcceptedAt(acceptedAt);
    entity.setMethodologyStatus(methodologyStatus);
    entity.setMappingVersion(mappingVersion);
    entity.setAssertionKey(assertionKey);
    return entity;
  }
}

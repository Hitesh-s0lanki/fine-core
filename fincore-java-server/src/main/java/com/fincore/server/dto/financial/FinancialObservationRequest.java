package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for financial observation. */
public record FinancialObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull UUID conceptId,
    @NotNull UUID filingId,
    UUID securityId,
    @NotNull @Pattern(regexp = "instant|duration|unknown") String periodType,
    LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    Integer sourceFiscalYear,
    @Size(max = 40000) String sourceFiscalPeriod,
    @Size(max = 40000) String sourceFrame,
    @NotNull @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @Size(max = 40000) String accountingStandard,
    @NotNull Object dimensions,
    @NotNull @Pattern(regexp = "reported|not_reported") String dimensionsStatus,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
    @NotBlank @Size(max = 40000) String unit,
    @NotNull Integer scale,
    @Size(max = 40000) String reportedDecimals,
    @NotBlank @Size(max = 40000) String reportedValueRaw,
    LocalDate filedOn,
    Instant acceptedAt,
    @Size(max = 40000) String contextId,
    @Size(max = 40000) String entityIdentifier,
    @NotBlank @Size(max = 40000) String assertionKey)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    FinancialObservation entity = new FinancialObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setConceptId(conceptId);
    entity.setFilingId(filingId);
    entity.setSecurityId(securityId);
    entity.setPeriodType(periodType);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setSourceFiscalYear(sourceFiscalYear);
    entity.setSourceFiscalPeriod(sourceFiscalPeriod);
    entity.setSourceFrame(sourceFrame);
    entity.setScope(scope);
    entity.setAccountingStandard(accountingStandard);
    entity.setDimensions(dimensions);
    entity.setDimensionsStatus(dimensionsStatus);
    entity.setValue(Decimals.parse(value));
    entity.setUnit(unit);
    entity.setScale(scale);
    entity.setReportedDecimals(reportedDecimals);
    entity.setReportedValueRaw(reportedValueRaw);
    entity.setFiledOn(filedOn);
    entity.setAcceptedAt(acceptedAt);
    entity.setContextId(contextId);
    entity.setEntityIdentifier(entityIdentifier);
    entity.setAssertionKey(assertionKey);
    return entity;
  }
}

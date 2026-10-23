package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.FinancialResultDisclosure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for financial result disclosure. */
public record FinancialResultDisclosureRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull UUID filingId,
    @NotBlank @Size(max = 40000) String sourceRecordId,
    LocalDate periodStart,
    LocalDate periodEnd,
    @Size(max = 40000) String periodLabelRaw,
    @Size(max = 40000) String fiscalYearLabelRaw,
    @NotNull @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @NotNull @Pattern(regexp = "audited|unaudited|reviewed|unknown") String auditStatus,
    @Size(max = 40000) String accountingStandard,
    Boolean cumulative,
    Instant broadcastAt)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    FinancialResultDisclosure entity = new FinancialResultDisclosure();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setFilingId(filingId);
    entity.setSourceRecordId(sourceRecordId);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setPeriodLabelRaw(periodLabelRaw);
    entity.setFiscalYearLabelRaw(fiscalYearLabelRaw);
    entity.setScope(scope);
    entity.setAuditStatus(auditStatus);
    entity.setAccountingStandard(accountingStandard);
    entity.setCumulative(cumulative);
    entity.setBroadcastAt(broadcastAt);
    return entity;
  }
}

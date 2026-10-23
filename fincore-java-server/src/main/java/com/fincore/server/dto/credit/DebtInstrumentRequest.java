package com.fincore.server.dto.credit;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.credit.DebtInstrument;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for debt instrument. */
public record DebtInstrumentRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotBlank @Size(max = 40000) String instrumentIdentifier,
    @NotBlank @Size(max = 40000) String currency,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String principal,
    @Size(max = 40000) String rateTerms,
    LocalDate maturity,
    @Size(max = 40000) String securitySeniority,
    @NotNull LocalDate asOfDate,
    UUID filingId)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    DebtInstrument entity = new DebtInstrument();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setInstrumentIdentifier(instrumentIdentifier);
    entity.setCurrency(currency);
    entity.setPrincipal(Decimals.parse(principal));
    entity.setRateTerms(rateTerms);
    entity.setMaturity(maturity);
    entity.setSecuritySeniority(securitySeniority);
    entity.setAsOfDate(asOfDate);
    entity.setFilingId(filingId);
    return entity;
  }
}

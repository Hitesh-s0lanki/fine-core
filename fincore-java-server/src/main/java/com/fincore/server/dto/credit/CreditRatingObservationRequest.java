package com.fincore.server.dto.credit;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.credit.CreditRatingObservation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for credit rating observation. */
public record CreditRatingObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    UUID debtInstrumentId,
    @NotBlank @Size(max = 40000) String agency,
    @NotBlank @Size(max = 40000) String ratingScale,
    @NotBlank @Size(max = 40000) String rating,
    @Size(max = 40000) String outlook,
    @NotNull LocalDate effectiveDate)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CreditRatingObservation entity = new CreditRatingObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setDebtInstrumentId(debtInstrumentId);
    entity.setAgency(agency);
    entity.setRatingScale(ratingScale);
    entity.setRating(rating);
    entity.setOutlook(outlook);
    entity.setEffectiveDate(effectiveDate);
    return entity;
  }
}

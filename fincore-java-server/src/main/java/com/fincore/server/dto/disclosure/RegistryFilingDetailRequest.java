package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.RegistryFilingDetail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for registry filing detail. */
public record RegistryFilingDetailRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID filingId,
    @NotNull UUID registrationId,
    @NotBlank @Size(max = 40000) String transactionId,
    @Size(max = 40000) String category,
    @Size(max = 40000) String filingType,
    @Size(max = 40000) String descriptionCode,
    @Size(max = 40000) String descriptionText,
    @NotNull Object descriptionValues,
    LocalDate actionDate,
    Integer pages)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    RegistryFilingDetail entity = new RegistryFilingDetail();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setFilingId(filingId);
    entity.setRegistrationId(registrationId);
    entity.setTransactionId(transactionId);
    entity.setCategory(category);
    entity.setFilingType(filingType);
    entity.setDescriptionCode(descriptionCode);
    entity.setDescriptionText(descriptionText);
    entity.setDescriptionValues(descriptionValues);
    entity.setActionDate(actionDate);
    entity.setPages(pages);
    return entity;
  }
}

package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.Filing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for filing. */
public record FilingRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull UUID providerId,
    @NotBlank @Size(max = 40000) String sourceFilingId,
    @Size(max = 40000) String formType,
    LocalDate filingDate,
    Instant acceptedAt,
    LocalDate reportDate,
    @Size(max = 40000) String title,
    @Size(max = 40000) String sourceUrl,
    Boolean isAmendment,
    UUID amendsFilingId,
    @NotNull @Pattern(regexp = "yes|no|unknown") String xbrlStatus)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    Filing entity = new Filing();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setProviderId(providerId);
    entity.setSourceFilingId(sourceFilingId);
    entity.setFormType(formType);
    entity.setFilingDate(filingDate);
    entity.setAcceptedAt(acceptedAt);
    entity.setReportDate(reportDate);
    entity.setTitle(title);
    entity.setSourceUrl(sourceUrl);
    entity.setIsAmendment(isAmendment);
    entity.setAmendsFilingId(amendsFilingId);
    entity.setXbrlStatus(xbrlStatus);
    return entity;
  }
}

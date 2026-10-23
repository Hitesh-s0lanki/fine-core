package com.fincore.server.dto.company;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.CompanyRegistration;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for company registration. */
public record CompanyRegistrationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotBlank @Size(max = 40000) String registry,
    @NotBlank @Size(max = 40000) String registryJurisdiction,
    @NotBlank @Size(max = 40000) String registrationNumber,
    @NotNull @Pattern(regexp = "domestic|overseas|branch|unknown") String registrationKind,
    @Size(max = 40000) String statusRaw,
    @Size(max = 40000) String statusNormalized,
    @Size(max = 40000) String legalForm,
    LocalDate incorporationDate,
    LocalDate establishmentDate,
    @Size(max = 40000) String homeJurisdiction,
    @Size(max = 40000) String parentRegistry,
    @Size(max = 40000) String governingLaw,
    @Size(max = 40000) String objects,
    LocalDate effectiveFrom,
    LocalDate effectiveTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CompanyRegistration entity = new CompanyRegistration();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setRegistry(registry);
    entity.setRegistryJurisdiction(registryJurisdiction);
    entity.setRegistrationNumber(registrationNumber);
    entity.setRegistrationKind(registrationKind);
    entity.setStatusRaw(statusRaw);
    entity.setStatusNormalized(statusNormalized);
    entity.setLegalForm(legalForm);
    entity.setIncorporationDate(incorporationDate);
    entity.setEstablishmentDate(establishmentDate);
    entity.setHomeJurisdiction(homeJurisdiction);
    entity.setParentRegistry(parentRegistry);
    entity.setGoverningLaw(governingLaw);
    entity.setObjects(objects);
    entity.setEffectiveFrom(effectiveFrom);
    entity.setEffectiveTo(effectiveTo);
    return entity;
  }
}

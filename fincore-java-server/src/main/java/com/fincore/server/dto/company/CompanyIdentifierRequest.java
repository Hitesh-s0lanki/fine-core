package com.fincore.server.dto.company;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.CompanyIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for company identifier. */
public record CompanyIdentifierRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull @Pattern(regexp = "cik|lei|cin|ein|provider_company_id|other") String scheme,
    @NotBlank @Size(max = 40000) String authority,
    @NotBlank @Size(max = 40000) String value,
    LocalDate validFrom,
    LocalDate validTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CompanyIdentifier entity = new CompanyIdentifier();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setScheme(scheme);
    entity.setAuthority(authority);
    entity.setValue(value);
    entity.setValidFrom(validFrom);
    entity.setValidTo(validTo);
    return entity;
  }
}

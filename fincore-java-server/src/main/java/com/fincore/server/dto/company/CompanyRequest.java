package com.fincore.server.dto.company;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for company. */
public record CompanyRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String slug,
    @NotBlank @Size(max = 40000) String displayName,
    @NotNull @Pattern(regexp = "issuer|subsidiary|other|unknown") String entityKind)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    Company entity = new Company();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSlug(slug);
    entity.setDisplayName(displayName);
    entity.setEntityKind(entityKind);
    return entity;
  }
}

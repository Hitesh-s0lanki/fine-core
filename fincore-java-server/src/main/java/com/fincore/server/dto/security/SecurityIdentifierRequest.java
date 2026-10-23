package com.fincore.server.dto.security;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.security.SecurityIdentifier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for security identifier. */
public record SecurityIdentifierRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID securityId,
    @NotNull @Pattern(regexp = "isin|cusip|sedol|provider_security_id|other") String scheme,
    @NotBlank @Size(max = 40000) String authority,
    @NotBlank @Size(max = 40000) String value,
    LocalDate validFrom,
    LocalDate validTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    SecurityIdentifier entity = new SecurityIdentifier();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSecurityId(securityId);
    entity.setScheme(scheme);
    entity.setAuthority(authority);
    entity.setValue(value);
    entity.setValidFrom(validFrom);
    entity.setValidTo(validTo);
    return entity;
  }
}

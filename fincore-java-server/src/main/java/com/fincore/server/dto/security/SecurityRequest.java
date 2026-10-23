package com.fincore.server.dto.security;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.security.Security;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for security. */
public record SecurityRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull @Pattern(regexp = "ordinary_share|adr|preferred_share|bond|other|unknown")
        String securityType,
    @Size(max = 40000) String shareClass,
    @Size(max = 40000) String name,
    Boolean isFund,
    Boolean isEtf)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    Security entity = new Security();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setSecurityType(securityType);
    entity.setShareClass(shareClass);
    entity.setName(name);
    entity.setIsFund(isFund);
    entity.setIsEtf(isEtf);
    return entity;
  }
}

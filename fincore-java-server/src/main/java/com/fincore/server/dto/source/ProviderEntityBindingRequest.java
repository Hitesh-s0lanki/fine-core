package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.ProviderEntityBinding;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for provider entity binding. */
public record ProviderEntityBindingRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID providerId,
    UUID companyId,
    UUID securityId,
    UUID listingId,
    @NotBlank @Size(max = 40000) String sourceIdentifier,
    @NotBlank @Size(max = 40000) String identifierKind,
    @NotNull @Pattern(regexp = "configured|verified|rejected") String verificationStatus,
    Instant verifiedAt,
    LocalDate validFrom,
    LocalDate validTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    ProviderEntityBinding entity = new ProviderEntityBinding();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setProviderId(providerId);
    entity.setCompanyId(companyId);
    entity.setSecurityId(securityId);
    entity.setListingId(listingId);
    entity.setSourceIdentifier(sourceIdentifier);
    entity.setIdentifierKind(identifierKind);
    entity.setVerificationStatus(verificationStatus);
    entity.setVerifiedAt(verifiedAt);
    entity.setValidFrom(validFrom);
    entity.setValidTo(validTo);
    return entity;
  }
}

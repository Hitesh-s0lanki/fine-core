package com.fincore.server.dto.governance;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.governance.OfficerParty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for officer party. */
public record OfficerPartyRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull @Pattern(regexp = "person|corporate|unknown") String partyKind,
    @NotBlank @Size(max = 40000) String displayName,
    @Size(max = 40000) String sourceOfficerId,
    @NotNull UUID providerId,
    @NotNull Object identifiers)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    OfficerParty entity = new OfficerParty();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setPartyKind(partyKind);
    entity.setDisplayName(displayName);
    entity.setSourceOfficerId(sourceOfficerId);
    entity.setProviderId(providerId);
    entity.setIdentifiers(identifiers);
    return entity;
  }
}

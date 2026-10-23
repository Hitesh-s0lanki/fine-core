package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.SourceEndpoint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for source endpoint. */
public record SourceEndpointRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID providerId,
    @NotBlank @Size(max = 40000) String code,
    @NotBlank @Size(max = 40000) String contractVersion,
    @Size(max = 40000) String documentationUrl,
    @NotNull @Pattern(regexp = "none|api_key|basic|other") String authScheme,
    @Size(max = 40000) String credentialReference,
    @NotNull Object supportedDomains,
    @NotNull @Pattern(regexp = "none|offset|page|cursor|archive_files|unknown")
        String paginationKind,
    @NotNull @Pattern(regexp = "unknown|available|auth_required|blocked") String accessStatus,
    @NotNull Instant checkedAt)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    SourceEndpoint entity = new SourceEndpoint();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setProviderId(providerId);
    entity.setCode(code);
    entity.setContractVersion(contractVersion);
    entity.setDocumentationUrl(documentationUrl);
    entity.setAuthScheme(authScheme);
    entity.setCredentialReference(credentialReference);
    entity.setSupportedDomains(supportedDomains);
    entity.setPaginationKind(paginationKind);
    entity.setAccessStatus(accessStatus);
    entity.setCheckedAt(checkedAt);
    return entity;
  }
}

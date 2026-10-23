package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.RawArtifact;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for raw artifact. */
public record RawArtifactRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String storageUri,
    @NotBlank @Size(max = 40000) String sha256,
    @NotNull @Pattern(regexp = "response_bytes|stored_bytes") String hashBasis,
    @NotBlank @Size(max = 40000) String contentType,
    @NotNull Long byteLength,
    @NotNull @Pattern(regexp = "json|html|csv|pdf|xbrl|text") String format,
    @NotNull Instant storedAt,
    @Size(max = 40000) String originalResponseSha256)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    RawArtifact entity = new RawArtifact();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setStorageUri(storageUri);
    entity.setSha256(sha256);
    entity.setHashBasis(hashBasis);
    entity.setContentType(contentType);
    entity.setByteLength(byteLength);
    entity.setFormat(format);
    entity.setStoredAt(storedAt);
    entity.setOriginalResponseSha256(originalResponseSha256);
    return entity;
  }
}

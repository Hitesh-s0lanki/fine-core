package com.fincore.server.dto.snapshot;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.snapshot.CompanySnapshot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for company snapshot. */
public record CompanySnapshotRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull Instant cutoffAt,
    @NotNull @Pattern(regexp = "collected_by_cutoff|published_by_cutoff|current_vintage")
        String knowledgeBasis,
    @NotBlank @Size(max = 40000) String selectionPolicyVersion,
    @NotBlank @Size(max = 40000) String mappingVersion,
    @NotBlank @Size(max = 40000) String manifestSha256,
    @NotNull @Pattern(regexp = "complete|partial|blocked") String status)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CompanySnapshot entity = new CompanySnapshot();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setCutoffAt(cutoffAt);
    entity.setKnowledgeBasis(knowledgeBasis);
    entity.setSelectionPolicyVersion(selectionPolicyVersion);
    entity.setMappingVersion(mappingVersion);
    entity.setManifestSha256(manifestSha256);
    entity.setStatus(status);
    return entity;
  }
}

package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.NormalizationRun;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for normalization run. */
public record NormalizationRunRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID ingestionAttemptId,
    @NotBlank @Size(max = 40000) String adapterId,
    @NotBlank @Size(max = 40000) String adapterVersion,
    @NotBlank @Size(max = 40000) String adapterContentHash,
    @NotNull Integer canonicalSchemaVersion,
    @NotBlank @Size(max = 40000) String mappingVersion,
    @NotNull Instant startedAt,
    Instant finishedAt,
    @NotNull @Pattern(regexp = "succeeded|partial|failed") String status,
    @NotNull Object outputRecordIds,
    @NotNull Object qualityIssueIds)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    NormalizationRun entity = new NormalizationRun();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setIngestionAttemptId(ingestionAttemptId);
    entity.setAdapterId(adapterId);
    entity.setAdapterVersion(adapterVersion);
    entity.setAdapterContentHash(adapterContentHash);
    entity.setCanonicalSchemaVersion(canonicalSchemaVersion);
    entity.setMappingVersion(mappingVersion);
    entity.setStartedAt(startedAt);
    entity.setFinishedAt(finishedAt);
    entity.setStatus(status);
    entity.setOutputRecordIds(outputRecordIds);
    entity.setQualityIssueIds(qualityIssueIds);
    return entity;
  }
}

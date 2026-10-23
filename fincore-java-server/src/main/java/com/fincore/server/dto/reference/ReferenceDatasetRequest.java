package com.fincore.server.dto.reference;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.reference.ReferenceDataset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for reference dataset. */
public record ReferenceDatasetRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String name,
    @NotBlank @Size(max = 40000) String publisher,
    LocalDate datasetDate,
    @NotNull Instant downloadedAt,
    @NotNull UUID artifactId,
    @Size(max = 40000) String methodologyUrl,
    @NotBlank @Size(max = 40000) String version)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    ReferenceDataset entity = new ReferenceDataset();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setName(name);
    entity.setPublisher(publisher);
    entity.setDatasetDate(datasetDate);
    entity.setDownloadedAt(downloadedAt);
    entity.setArtifactId(artifactId);
    entity.setMethodologyUrl(methodologyUrl);
    entity.setVersion(version);
    return entity;
  }
}

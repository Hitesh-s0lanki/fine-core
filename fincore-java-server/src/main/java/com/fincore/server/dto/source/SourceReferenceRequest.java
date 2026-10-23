package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.SourceReference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for source reference. */
public record SourceReferenceRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    UUID ingestionAttemptId,
    UUID artifactId,
    UUID normalizationRunId,
    @Size(max = 40000) String sourceUrl,
    @Size(max = 40000) String locator,
    @NotNull @Pattern(regexp = "api|html_extract|csv_conversion|document_parse|configured|manual")
        String extractionMethod,
    @Size(max = 40000) String extractorVersion,
    @NotNull Instant observedAt,
    Instant availableAt,
    @Pattern(regexp = "timestamp|date|unknown") String availabilityPrecision,
    @Size(max = 40000) String availabilityBasis)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    SourceReference entity = new SourceReference();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setIngestionAttemptId(ingestionAttemptId);
    entity.setArtifactId(artifactId);
    entity.setNormalizationRunId(normalizationRunId);
    entity.setSourceUrl(sourceUrl);
    entity.setLocator(locator);
    entity.setExtractionMethod(extractionMethod);
    entity.setExtractorVersion(extractorVersion);
    entity.setObservedAt(observedAt);
    entity.setAvailableAt(availableAt);
    entity.setAvailabilityPrecision(availabilityPrecision);
    entity.setAvailabilityBasis(availabilityBasis);
    return entity;
  }
}

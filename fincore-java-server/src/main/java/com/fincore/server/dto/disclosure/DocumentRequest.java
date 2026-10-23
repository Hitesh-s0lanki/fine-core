package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for document. */
public record DocumentRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String url,
    @Size(max = 40000) String title,
    Instant publishedAt,
    @Size(max = 40000) String language,
    @Size(max = 40000) String mediaType,
    @NotNull @Pattern(regexp = "not_requested|downloaded|failed") String downloadStatus,
    UUID artifactId,
    @NotNull @Pattern(regexp = "not_requested|parsed|failed") String parseStatus,
    @Size(max = 40000) String parserVersion)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    Document entity = new Document();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setUrl(url);
    entity.setTitle(title);
    entity.setPublishedAt(publishedAt);
    entity.setLanguage(language);
    entity.setMediaType(mediaType);
    entity.setDownloadStatus(downloadStatus);
    entity.setArtifactId(artifactId);
    entity.setParseStatus(parseStatus);
    entity.setParserVersion(parserVersion);
    return entity;
  }
}

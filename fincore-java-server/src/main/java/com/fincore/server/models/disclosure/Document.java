package com.fincore.server.models.disclosure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.RawArtifact;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent document assertion. */
@Entity
@Table(name = "document")
@DiscriminatorValue("documents")
@Getter
@Setter
@NoArgsConstructor
public class Document extends DomainRecord {
  @Column(name = "url", nullable = false)
  private String url;

  @Column(name = "title", nullable = true)
  private String title;

  @Column(name = "published_at", nullable = true)
  private Instant publishedAt;

  @Column(name = "language", nullable = true)
  private String language;

  @Column(name = "media_type", nullable = true)
  private String mediaType;

  @Column(name = "download_status", nullable = false)
  private String downloadStatus;

  @Column(name = "artifact_id", nullable = true)
  private UUID artifactId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artifact_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private RawArtifact artifactReference;

  @Column(name = "parse_status", nullable = false)
  private String parseStatus;

  @Column(name = "parser_version", nullable = true)
  private String parserVersion;
}

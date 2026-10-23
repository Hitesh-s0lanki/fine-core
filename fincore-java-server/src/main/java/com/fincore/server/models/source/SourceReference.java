package com.fincore.server.models.source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
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

/** Persistent source reference assertion. */
@Entity
@Table(name = "source_reference")
@DiscriminatorValue("source-references")
@Getter
@Setter
@NoArgsConstructor
public class SourceReference extends DomainRecord {
  @Column(name = "ingestion_attempt_id", nullable = true)
  private UUID ingestionAttemptId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingestion_attempt_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private IngestionAttempt ingestionAttemptReference;

  @Column(name = "artifact_id", nullable = true)
  private UUID artifactId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artifact_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private RawArtifact artifactReference;

  @Column(name = "normalization_run_id", nullable = true)
  private UUID normalizationRunId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "normalization_run_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private NormalizationRun normalizationRunReference;

  @Column(name = "source_url", nullable = true)
  private String sourceUrl;

  @Column(name = "locator", nullable = true)
  private String locator;

  @Column(name = "extraction_method", nullable = false)
  private String extractionMethod;

  @Column(name = "extractor_version", nullable = true)
  private String extractorVersion;

  @Column(name = "observed_at", nullable = false)
  private Instant observedAt;

  @Column(name = "available_at", nullable = true)
  private Instant availableAt;

  @Column(name = "availability_precision", nullable = true)
  private String availabilityPrecision;

  @Column(name = "availability_basis", nullable = true)
  private String availabilityBasis;
}

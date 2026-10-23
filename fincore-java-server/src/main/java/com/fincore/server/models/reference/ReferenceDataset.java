package com.fincore.server.models.reference;

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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent reference dataset assertion. */
@Entity
@Table(name = "reference_dataset")
@DiscriminatorValue("reference-datasets")
@Getter
@Setter
@NoArgsConstructor
public class ReferenceDataset extends DomainRecord {
  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "publisher", nullable = false)
  private String publisher;

  @Column(name = "dataset_date", nullable = true)
  private LocalDate datasetDate;

  @Column(name = "downloaded_at", nullable = false)
  private Instant downloadedAt;

  @Column(name = "artifact_id", nullable = false)
  private UUID artifactId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artifact_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private RawArtifact artifactReference;

  @Column(name = "methodology_url", nullable = true)
  private String methodologyUrl;

  @Column(name = "version", nullable = false)
  private String version;
}

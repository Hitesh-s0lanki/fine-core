package com.fincore.server.models.reference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent reference dataset observation assertion. */
@Entity
@Table(name = "reference_dataset_observation")
@DiscriminatorValue("reference-dataset-observations")
@Getter
@Setter
@NoArgsConstructor
public class ReferenceDatasetObservation extends DomainRecord {
  @Column(name = "dataset_id", nullable = false)
  private UUID datasetId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "dataset_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private ReferenceDataset datasetReference;

  @Column(name = "geography", nullable = true)
  private String geography;

  @Column(name = "industry_scheme", nullable = true)
  private String industryScheme;

  @Column(name = "industry_label", nullable = true)
  private String industryLabel;

  @Column(name = "metric_code", nullable = false)
  private String metricCode;

  @Column(name = "value", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal value;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "sample_size", nullable = true)
  private Integer sampleSize;

  @Column(name = "methodology_notes", nullable = true)
  private String methodologyNotes;

  @Column(name = "as_of_date", nullable = true)
  private LocalDate asOfDate;
}

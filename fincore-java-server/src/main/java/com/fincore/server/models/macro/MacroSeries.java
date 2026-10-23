package com.fincore.server.models.macro;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.SourceProvider;
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

/** Persistent macro series assertion. */
@Entity
@Table(name = "macro_series")
@DiscriminatorValue("macro-series")
@Getter
@Setter
@NoArgsConstructor
public class MacroSeries extends DomainRecord {
  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "source_series_id", nullable = false)
  private String sourceSeriesId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "geography", nullable = false)
  private String geography;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "frequency", nullable = false)
  private String frequency;

  @Column(name = "seasonal_adjustment", nullable = true)
  private String seasonalAdjustment;

  @Column(name = "transformation", nullable = false)
  private String transformation;

  @Column(name = "unit_multiplier", nullable = true)
  private Integer unitMultiplier;

  @Column(name = "notes", nullable = true)
  private String notes;

  @Column(name = "last_updated_at", nullable = true)
  private Instant lastUpdatedAt;
}

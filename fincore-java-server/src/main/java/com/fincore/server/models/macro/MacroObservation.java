package com.fincore.server.models.macro;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent macro observation assertion. */
@Entity
@Table(name = "macro_observation")
@DiscriminatorValue("macro-observations")
@Getter
@Setter
@NoArgsConstructor
public class MacroObservation extends DomainRecord {
  @Column(name = "series_id", nullable = false)
  private UUID seriesId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "series_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private MacroSeries seriesReference;

  @Column(name = "observation_date", nullable = false)
  private LocalDate observationDate;

  @Column(name = "value", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal value;

  @Column(name = "missing_reason", nullable = true)
  private String missingReason;

  @Column(name = "vintage_date", nullable = true)
  private LocalDate vintageDate;

  @Column(name = "vintage_basis", nullable = false)
  private String vintageBasis;

  @Column(name = "released_at", nullable = true)
  private Instant releasedAt;

  @Column(name = "realtime_start", nullable = true)
  private LocalDate realtimeStart;

  @Column(name = "realtime_end", nullable = true)
  private LocalDate realtimeEnd;
}

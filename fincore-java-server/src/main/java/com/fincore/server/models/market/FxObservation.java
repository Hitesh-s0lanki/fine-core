package com.fincore.server.models.market;

import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent fx observation assertion. */
@Entity
@Table(name = "fx_observation")
@DiscriminatorValue("fx-observations")
@Getter
@Setter
@NoArgsConstructor
public class FxObservation extends DomainRecord {
  @Column(name = "base_currency", nullable = false)
  private String baseCurrency;

  @Column(name = "quote_currency", nullable = false)
  private String quoteCurrency;

  @Column(name = "observed_at", nullable = false)
  private Instant observedAt;

  @Column(name = "rate", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal rate;

  @Column(name = "rate_kind", nullable = false)
  private String rateKind;

  @Column(name = "period_start", nullable = true)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = true)
  private LocalDate periodEnd;
}

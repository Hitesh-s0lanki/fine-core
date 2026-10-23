package com.fincore.server.models.market;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.security.Listing;
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

/** Persistent price bar assertion. */
@Entity
@Table(name = "price_bar")
@DiscriminatorValue("price-bars")
@Getter
@Setter
@NoArgsConstructor
public class PriceBar extends DomainRecord {
  @Column(name = "listing_id", nullable = false)
  private UUID listingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Listing listingReference;

  @Column(name = "interval", nullable = false)
  private String interval;

  @Column(name = "trading_date", nullable = true)
  private LocalDate tradingDate;

  @Column(name = "exchange_timezone", nullable = false)
  private String exchangeTimezone;

  @Column(name = "period_start", nullable = true)
  private Instant periodStart;

  @Column(name = "period_end", nullable = true)
  private Instant periodEnd;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "open", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal open;

  @Column(name = "high", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal high;

  @Column(name = "low", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal low;

  @Column(name = "close", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal close;

  @Column(name = "volume", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal volume;

  @Column(name = "adjustment_basis", nullable = false)
  private String adjustmentBasis;

  @Column(name = "adjustment_version", nullable = true)
  private String adjustmentVersion;

  @Column(name = "adjusted_close", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal adjustedClose;

  @Column(name = "adjusted_close_basis", nullable = true)
  private String adjustedCloseBasis;
}

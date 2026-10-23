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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent market quote assertion. */
@Entity
@Table(name = "market_quote")
@DiscriminatorValue("market-quotes")
@Getter
@Setter
@NoArgsConstructor
public class MarketQuote extends DomainRecord {
  @Column(name = "listing_id", nullable = false)
  private UUID listingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Listing listingReference;

  @Column(name = "as_of", nullable = false)
  private Instant asOf;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "last_price", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal lastPrice;

  @Column(name = "bid", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal bid;

  @Column(name = "ask", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal ask;

  @Column(name = "volume", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal volume;

  @Column(name = "market_status", nullable = true)
  private String marketStatus;

  @Column(name = "delay_seconds", nullable = true)
  private Integer delaySeconds;
}

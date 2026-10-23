package com.fincore.server.models.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent listing assertion. */
@Entity
@Table(name = "listing")
@DiscriminatorValue("listings")
@Getter
@Setter
@NoArgsConstructor
public class Listing extends DomainRecord {
  @Column(name = "security_id", nullable = false)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @Column(name = "exchange_code", nullable = false)
  private String exchangeCode;

  @Column(name = "exchange_mic", nullable = true)
  private String exchangeMic;

  @Column(name = "symbol", nullable = false)
  private String symbol;

  @Column(name = "trading_currency", nullable = true)
  private String tradingCurrency;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "first_trade_date", nullable = true)
  private LocalDate firstTradeDate;

  @Column(name = "valid_from", nullable = true)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = true)
  private LocalDate validTo;
}

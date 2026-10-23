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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent depositary receipt ratio assertion. */
@Entity
@Table(name = "depositary_receipt_ratio")
@DiscriminatorValue("depositary-receipt-ratios")
@Getter
@Setter
@NoArgsConstructor
public class DepositaryReceiptRatio extends DomainRecord {
  @Column(name = "receipt_security_id", nullable = false)
  private UUID receiptSecurityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receipt_security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security receiptSecurityReference;

  @Column(name = "underlying_security_id", nullable = false)
  private UUID underlyingSecurityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "underlying_security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security underlyingSecurityReference;

  @Column(name = "receipt_units", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal receiptUnits;

  @Column(name = "underlying_units", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal underlyingUnits;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to", nullable = true)
  private LocalDate effectiveTo;
}

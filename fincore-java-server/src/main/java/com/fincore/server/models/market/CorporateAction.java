package com.fincore.server.models.market;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.security.Security;
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

/** Persistent corporate action assertion. */
@Entity
@Table(name = "corporate_action")
@DiscriminatorValue("corporate-actions")
@Getter
@Setter
@NoArgsConstructor
public class CorporateAction extends DomainRecord {
  @Column(name = "security_id", nullable = false)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @Column(name = "action_type", nullable = false)
  private String actionType;

  @Column(name = "announced_at", nullable = true)
  private Instant announcedAt;

  @Column(name = "ex_date", nullable = true)
  private LocalDate exDate;

  @Column(name = "record_date", nullable = true)
  private LocalDate recordDate;

  @Column(name = "payment_date", nullable = true)
  private LocalDate paymentDate;

  @Column(name = "amount", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal amount;

  @Column(name = "currency", nullable = true)
  private String currency;

  @Column(name = "ratio_numerator", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal ratioNumerator;

  @Column(name = "ratio_denominator", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal ratioDenominator;
}

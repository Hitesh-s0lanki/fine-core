package com.fincore.server.models.governance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.security.Security;
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

/** Persistent ownership observation assertion. */
@Entity
@Table(name = "ownership_observation")
@DiscriminatorValue("ownership-observations")
@Getter
@Setter
@NoArgsConstructor
public class OwnershipObservation extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "security_id", nullable = true)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @Column(name = "holder_name", nullable = false)
  private String holderName;

  @Column(name = "holder_company_id", nullable = true)
  private UUID holderCompanyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "holder_company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company holderCompanyReference;

  @Column(name = "as_of_date", nullable = false)
  private LocalDate asOfDate;

  @Column(name = "shares", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal shares;

  @Column(name = "ownership_percent", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal ownershipPercent;

  @Column(name = "voting_percent", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal votingPercent;
}

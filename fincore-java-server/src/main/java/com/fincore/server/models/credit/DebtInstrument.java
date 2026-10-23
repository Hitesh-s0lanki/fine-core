package com.fincore.server.models.credit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.disclosure.Filing;
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

/** Persistent debt instrument assertion. */
@Entity
@Table(name = "debt_instrument")
@DiscriminatorValue("debt-instruments")
@Getter
@Setter
@NoArgsConstructor
public class DebtInstrument extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "instrument_identifier", nullable = false)
  private String instrumentIdentifier;

  @Column(name = "currency", nullable = false)
  private String currency;

  @Column(name = "principal", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal principal;

  @Column(name = "rate_terms", nullable = true)
  private String rateTerms;

  @Column(name = "maturity", nullable = true)
  private LocalDate maturity;

  @Column(name = "security_seniority", nullable = true)
  private String securitySeniority;

  @Column(name = "as_of_date", nullable = false)
  private LocalDate asOfDate;

  @Column(name = "filing_id", nullable = true)
  private UUID filingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing filingReference;
}

package com.fincore.server.models.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.disclosure.Filing;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent financial observation assertion. */
@Entity
@Table(name = "financial_observation")
@DiscriminatorValue("financial-observations")
@Getter
@Setter
@NoArgsConstructor
public class FinancialObservation extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "concept_id", nullable = false)
  private UUID conceptId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concept_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private FinancialConcept conceptReference;

  @Column(name = "filing_id", nullable = false)
  private UUID filingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing filingReference;

  @Column(name = "security_id", nullable = true)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @Column(name = "period_type", nullable = false)
  private String periodType;

  @Column(name = "period_start", nullable = true)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "source_fiscal_year", nullable = true)
  private Integer sourceFiscalYear;

  @Column(name = "source_fiscal_period", nullable = true)
  private String sourceFiscalPeriod;

  @Column(name = "source_frame", nullable = true)
  private String sourceFrame;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "accounting_standard", nullable = true)
  private String accountingStandard;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dimensions", nullable = false, columnDefinition = "jsonb")
  private Object dimensions;

  @Column(name = "dimensions_status", nullable = false)
  private String dimensionsStatus;

  @Column(name = "value", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal value;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "scale", nullable = false)
  private Integer scale;

  @Column(name = "reported_decimals", nullable = true)
  private String reportedDecimals;

  @Column(name = "reported_value_raw", nullable = false)
  private String reportedValueRaw;

  @Column(name = "filed_on", nullable = true)
  private LocalDate filedOn;

  @Column(name = "accepted_at", nullable = true)
  private Instant acceptedAt;

  @Column(name = "context_id", nullable = true)
  private String contextId;

  @Column(name = "entity_identifier", nullable = true)
  private String entityIdentifier;

  @Column(name = "assertion_key", nullable = false)
  private String assertionKey;
}

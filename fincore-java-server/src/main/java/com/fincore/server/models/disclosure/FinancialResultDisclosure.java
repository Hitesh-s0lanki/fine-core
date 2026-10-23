package com.fincore.server.models.disclosure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent financial result disclosure assertion. */
@Entity
@Table(name = "financial_result_disclosure")
@DiscriminatorValue("financial-results")
@Getter
@Setter
@NoArgsConstructor
public class FinancialResultDisclosure extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "filing_id", nullable = false)
  private UUID filingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing filingReference;

  @Column(name = "source_record_id", nullable = false)
  private String sourceRecordId;

  @Column(name = "period_start", nullable = true)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = true)
  private LocalDate periodEnd;

  @Column(name = "period_label_raw", nullable = true)
  private String periodLabelRaw;

  @Column(name = "fiscal_year_label_raw", nullable = true)
  private String fiscalYearLabelRaw;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "audit_status", nullable = false)
  private String auditStatus;

  @Column(name = "accounting_standard", nullable = true)
  private String accountingStandard;

  @Column(name = "cumulative", nullable = true)
  private Boolean cumulative;

  @Column(name = "broadcast_at", nullable = true)
  private Instant broadcastAt;
}

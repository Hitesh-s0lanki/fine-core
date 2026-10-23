package com.fincore.server.models.disclosure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.source.SourceProvider;
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

/** Persistent filing assertion. */
@Entity
@Table(name = "filing")
@DiscriminatorValue("filings")
@Getter
@Setter
@NoArgsConstructor
public class Filing extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "source_filing_id", nullable = false)
  private String sourceFilingId;

  @Column(name = "form_type", nullable = true)
  private String formType;

  @Column(name = "filing_date", nullable = true)
  private LocalDate filingDate;

  @Column(name = "accepted_at", nullable = true)
  private Instant acceptedAt;

  @Column(name = "report_date", nullable = true)
  private LocalDate reportDate;

  @Column(name = "title", nullable = true)
  private String title;

  @Column(name = "source_url", nullable = true)
  private String sourceUrl;

  @Column(name = "is_amendment", nullable = true)
  private Boolean isAmendment;

  @Column(name = "amends_filing_id", nullable = true)
  private UUID amendsFilingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "amends_filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing amendsFilingReference;

  @Column(name = "xbrl_status", nullable = false)
  private String xbrlStatus;
}

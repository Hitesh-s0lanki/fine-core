package com.fincore.server.models.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.disclosure.Filing;
import com.fincore.server.models.security.Listing;
import com.fincore.server.models.security.Security;
import com.fincore.server.models.source.SourceEndpoint;
import com.fincore.server.models.source.SourceProvider;
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

/** Persistent vendor financial observation assertion. */
@Entity
@Table(name = "vendor_financial_observation")
@DiscriminatorValue("vendor-financial-observations")
@Getter
@Setter
@NoArgsConstructor
public class VendorFinancialObservation extends DomainRecord {
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

  @Column(name = "listing_id", nullable = true)
  private UUID listingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Listing listingReference;

  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "source_endpoint_id", nullable = false)
  private UUID sourceEndpointId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_endpoint_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceEndpoint sourceEndpointReference;

  @Column(name = "source_record_id", nullable = false)
  private String sourceRecordId;

  @Column(name = "source_field", nullable = false)
  private String sourceField;

  @Column(name = "metric_id", nullable = true)
  private UUID metricId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private MetricDefinition metricReference;

  @Column(name = "period_start", nullable = true)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "period_type", nullable = false)
  private String periodType;

  @Column(name = "period_label_raw", nullable = true)
  private String periodLabelRaw;

  @Column(name = "reported_currency", nullable = true)
  private String reportedCurrency;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "value", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal value;

  @Column(name = "scale", nullable = false)
  private Integer scale;

  @Column(name = "reported_value_raw", nullable = false)
  private String reportedValueRaw;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "source_filing_id", nullable = true)
  private String sourceFilingId;

  @Column(name = "filing_id", nullable = true)
  private UUID filingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing filingReference;

  @Column(name = "filed_on", nullable = true)
  private LocalDate filedOn;

  @Column(name = "accepted_at", nullable = true)
  private Instant acceptedAt;

  @Column(name = "methodology_status", nullable = false)
  private String methodologyStatus;

  @Column(name = "mapping_version", nullable = true)
  private String mappingVersion;

  @Column(name = "assertion_key", nullable = false)
  private String assertionKey;
}

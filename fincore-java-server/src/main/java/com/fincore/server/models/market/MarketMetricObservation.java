package com.fincore.server.models.market;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.security.Listing;
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

/** Persistent market metric observation assertion. */
@Entity
@Table(name = "market_metric_observation")
@DiscriminatorValue("market-metric-observations")
@Getter
@Setter
@NoArgsConstructor
public class MarketMetricObservation extends DomainRecord {
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

  @Column(name = "metric_code", nullable = false)
  private String metricCode;

  @Column(name = "metric_definition", nullable = false)
  private String metricDefinition;

  @Column(name = "value", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal value;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "currency", nullable = true)
  private String currency;

  @Column(name = "as_of", nullable = true)
  private Instant asOf;

  @Column(name = "reference_period_start", nullable = true)
  private LocalDate referencePeriodStart;

  @Column(name = "reference_period_end", nullable = true)
  private LocalDate referencePeriodEnd;

  @Column(name = "methodology", nullable = true)
  private String methodology;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_record_refs", nullable = false, columnDefinition = "jsonb")
  private Object inputRecordRefs;
}

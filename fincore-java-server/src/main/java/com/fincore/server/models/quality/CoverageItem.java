package com.fincore.server.models.quality;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.financial.MetricDefinition;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent coverage item assertion. */
@Entity
@Table(name = "coverage_item")
@DiscriminatorValue("coverage-items")
@Getter
@Setter
@NoArgsConstructor
public class CoverageItem extends DomainRecord {
  @Column(name = "company_id", nullable = true)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "provider_id", nullable = true)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "domain", nullable = false)
  private String domain;

  @Column(name = "endpoint", nullable = true)
  private String endpoint;

  @Column(name = "metric_id", nullable = true)
  private UUID metricId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private MetricDefinition metricReference;

  @Column(name = "period_start", nullable = true)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = true)
  private LocalDate periodEnd;

  @Column(name = "scope", nullable = true)
  private String scope;

  @Column(name = "unit", nullable = true)
  private String unit;

  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "reason_code", nullable = true)
  private String reasonCode;

  @Column(name = "message", nullable = true)
  private String message;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ingestion_attempt_ids", nullable = false, columnDefinition = "jsonb")
  private Object ingestionAttemptIds;

  @Column(name = "latest_observation_date", nullable = true)
  private LocalDate latestObservationDate;

  @Column(name = "expected_through", nullable = true)
  private LocalDate expectedThrough;

  @Column(name = "freshness_policy_version", nullable = true)
  private String freshnessPolicyVersion;
}

package com.fincore.server.models.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.security.Security;
import com.fincore.server.models.snapshot.CompanySnapshot;
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

/** Persistent derived metric observation assertion. */
@Entity
@Table(name = "derived_metric_observation")
@DiscriminatorValue("derived-metric-observations")
@Getter
@Setter
@NoArgsConstructor
public class DerivedMetricObservation extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "metric_id", nullable = false)
  private UUID metricId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private MetricDefinition metricReference;

  @Column(name = "snapshot_id", nullable = false)
  private UUID snapshotId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private CompanySnapshot snapshotReference;

  @Column(name = "security_id", nullable = true)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dimensions", nullable = false, columnDefinition = "jsonb")
  private Object dimensions;

  @Column(name = "period_start", nullable = true)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "value", nullable = false, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal value;

  @Column(name = "unit", nullable = false)
  private String unit;

  @Column(name = "scale", nullable = false)
  private Integer scale;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_record_refs", nullable = false, columnDefinition = "jsonb")
  private Object inputRecordRefs;

  @Column(name = "method", nullable = false)
  private String method;

  @Column(name = "method_version", nullable = false)
  private String methodVersion;

  @Column(name = "computed_at", nullable = false)
  private Instant computedAt;
}

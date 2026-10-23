package com.fincore.server.models.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.DerivedMetricObservation;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.models.financial.MetricDefinition;
import com.fincore.server.models.financial.VendorFinancialObservation;
import com.fincore.server.models.security.Security;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent snapshot financial cell assertion. */
@Entity
@Table(name = "snapshot_financial_cell")
@DiscriminatorValue("snapshot-financial-cells")
@Getter
@Setter
@NoArgsConstructor
public class SnapshotFinancialCell extends DomainRecord {
  @Column(name = "snapshot_id", nullable = false)
  private UUID snapshotId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private CompanySnapshot snapshotReference;

  @Column(name = "metric_id", nullable = false)
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

  @Column(name = "scope", nullable = false)
  private String scope;

  @Column(name = "unit", nullable = false)
  private String unit;

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

  @Column(name = "financial_observation_id", nullable = true)
  private UUID financialObservationId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "financial_observation_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private FinancialObservation financialObservationReference;

  @Column(name = "vendor_financial_observation_id", nullable = true)
  private UUID vendorFinancialObservationId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendor_financial_observation_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private VendorFinancialObservation vendorFinancialObservationReference;

  @Column(name = "derived_metric_observation_id", nullable = true)
  private UUID derivedMetricObservationId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "derived_metric_observation_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private DerivedMetricObservation derivedMetricObservationReference;

  @Column(name = "missing_reason", nullable = true)
  private String missingReason;

  @Column(name = "cell_key", nullable = false)
  private String cellKey;
}

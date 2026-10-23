package com.fincore.server.models.quality;

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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent data quality issue assertion. */
@Entity
@Table(name = "data_quality_issue")
@DiscriminatorValue("quality-issues")
@Getter
@Setter
@NoArgsConstructor
public class DataQualityIssue extends DomainRecord {
  @Column(name = "company_id", nullable = true)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "record_type", nullable = false)
  private String recordType;

  @Column(name = "record_id", nullable = true)
  private UUID recordId;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "severity", nullable = false)
  private String severity;

  @Column(name = "message", nullable = false)
  private String message;

  @Column(name = "detected_at", nullable = false)
  private Instant detectedAt;

  @Column(name = "rule_version", nullable = false)
  private String ruleVersion;

  @Column(name = "resolution", nullable = true)
  private String resolution;
}

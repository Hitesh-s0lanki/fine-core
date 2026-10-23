package com.fincore.server.models.source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent normalization run assertion. */
@Entity
@Table(name = "normalization_run")
@DiscriminatorValue("normalization-runs")
@Getter
@Setter
@NoArgsConstructor
public class NormalizationRun extends DomainRecord {
  @Column(name = "ingestion_attempt_id", nullable = false)
  private UUID ingestionAttemptId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ingestion_attempt_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private IngestionAttempt ingestionAttemptReference;

  @Column(name = "adapter_id", nullable = false)
  private String adapterId;

  @Column(name = "adapter_version", nullable = false)
  private String adapterVersion;

  @Column(name = "adapter_content_hash", nullable = false)
  private String adapterContentHash;

  @Column(name = "canonical_schema_version", nullable = false)
  private Integer canonicalSchemaVersion;

  @Column(name = "mapping_version", nullable = false)
  private String mappingVersion;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "finished_at", nullable = true)
  private Instant finishedAt;

  @Column(name = "status", nullable = false)
  private String status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "output_record_ids", nullable = false, columnDefinition = "jsonb")
  private Object outputRecordIds;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "quality_issue_ids", nullable = false, columnDefinition = "jsonb")
  private Object qualityIssueIds;
}

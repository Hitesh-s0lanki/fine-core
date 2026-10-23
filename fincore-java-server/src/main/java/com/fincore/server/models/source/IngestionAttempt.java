package com.fincore.server.models.source;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent ingestion attempt assertion. */
@Entity
@Table(name = "ingestion_attempt")
@DiscriminatorValue("ingestion-attempts")
@Getter
@Setter
@NoArgsConstructor
public class IngestionAttempt extends DomainRecord {
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

  @Column(name = "provider_entity_binding_id", nullable = true)
  private UUID providerEntityBindingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_entity_binding_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private ProviderEntityBinding providerEntityBindingReference;

  @Column(name = "company_id", nullable = true)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "endpoint", nullable = false)
  private String endpoint;

  @Column(name = "request_url_redacted", nullable = false)
  private String requestUrlRedacted;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "retrieved_at", nullable = true)
  private Instant retrievedAt;

  @Column(name = "http_status", nullable = true)
  private Integer httpStatus;

  @Column(name = "outcome", nullable = false)
  private String outcome;

  @Column(name = "error_code", nullable = true)
  private String errorCode;

  @Column(name = "error_message", nullable = true)
  private String errorMessage;

  @Column(name = "artifact_id", nullable = true)
  private UUID artifactId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "artifact_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private RawArtifact artifactReference;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "request_scope", nullable = false, columnDefinition = "jsonb")
  private Object requestScope;

  @Column(name = "next_cursor", nullable = true)
  private String nextCursor;

  @Column(name = "pagination_complete", nullable = true)
  private Boolean paginationComplete;
}

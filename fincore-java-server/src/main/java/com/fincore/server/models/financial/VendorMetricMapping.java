package com.fincore.server.models.financial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.SourceEndpoint;
import com.fincore.server.models.source.SourceProvider;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent vendor metric mapping assertion. */
@Entity
@Table(name = "vendor_metric_mapping")
@DiscriminatorValue("vendor-metric-mappings")
@Getter
@Setter
@NoArgsConstructor
public class VendorMetricMapping extends DomainRecord {
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

  @Column(name = "source_field", nullable = false)
  private String sourceField;

  @Column(name = "metric_id", nullable = false)
  private UUID metricId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private MetricDefinition metricReference;

  @Column(name = "mapping_version", nullable = false)
  private String mappingVersion;

  @Column(name = "definition_status", nullable = false)
  private String definitionStatus;

  @Column(name = "semantic_notes", nullable = false)
  private String semanticNotes;

  @Column(name = "transform_method", nullable = true)
  private String transformMethod;
}

package com.fincore.server.models.financial;

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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent concept metric mapping assertion. */
@Entity
@Table(name = "concept_metric_mapping")
@DiscriminatorValue("concept-metric-mappings")
@Getter
@Setter
@NoArgsConstructor
public class ConceptMetricMapping extends DomainRecord {
  @Column(name = "concept_id", nullable = false)
  private UUID conceptId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "concept_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private FinancialConcept conceptReference;

  @Column(name = "metric_id", nullable = false)
  private UUID metricId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private MetricDefinition metricReference;

  @Column(name = "company_id", nullable = true)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "scope_constraint", nullable = true)
  private String scopeConstraint;

  @Column(name = "accounting_standard", nullable = true)
  private String accountingStandard;

  @Column(name = "mapping_version", nullable = false)
  private String mappingVersion;

  @Column(name = "semantic_notes", nullable = false)
  private String semanticNotes;

  @Column(name = "transform_method", nullable = true)
  private String transformMethod;

  @Column(name = "valid_from", nullable = true)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = true)
  private LocalDate validTo;
}

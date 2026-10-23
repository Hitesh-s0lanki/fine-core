package com.fincore.server.models.financial;

import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent financial concept assertion. */
@Entity
@Table(name = "financial_concept")
@DiscriminatorValue("financial-concepts")
@Getter
@Setter
@NoArgsConstructor
public class FinancialConcept extends DomainRecord {
  @Column(name = "taxonomy", nullable = false)
  private String taxonomy;

  @Column(name = "taxonomy_version", nullable = true)
  private String taxonomyVersion;

  @Column(name = "concept_name", nullable = false)
  private String conceptName;

  @Column(name = "label", nullable = true)
  private String label;

  @Column(name = "description", nullable = true)
  private String description;

  @Column(name = "period_type", nullable = false)
  private String periodType;
}

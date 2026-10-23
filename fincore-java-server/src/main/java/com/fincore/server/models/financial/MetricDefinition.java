package com.fincore.server.models.financial;

import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent metric definition assertion. */
@Entity
@Table(name = "metric_definition")
@DiscriminatorValue("metric-definitions")
@Getter
@Setter
@NoArgsConstructor
public class MetricDefinition extends DomainRecord {
  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "statement", nullable = false)
  private String statement;

  @Column(name = "period_type", nullable = false)
  private String periodType;

  @Column(name = "unit_kind", nullable = false)
  private String unitKind;

  @Column(name = "definition", nullable = false)
  private String definition;

  @Column(name = "sign_convention", nullable = false)
  private String signConvention;
}

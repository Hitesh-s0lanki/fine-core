package com.fincore.server.models.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent company relationship assertion. */
@Entity
@Table(name = "company_relationship")
@DiscriminatorValue("company-relationships")
@Getter
@Setter
@NoArgsConstructor
public class CompanyRelationship extends DomainRecord {
  @Column(name = "subject_company_id", nullable = false)
  private UUID subjectCompanyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subject_company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company subjectCompanyReference;

  @Column(name = "object_company_id", nullable = false)
  private UUID objectCompanyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "object_company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company objectCompanyReference;

  @Column(name = "relationship_type", nullable = false)
  private String relationshipType;

  @Column(name = "ownership_percent", nullable = true, precision = 60, scale = 18)
  @com.fasterxml.jackson.annotation.JsonFormat(
      shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
  private BigDecimal ownershipPercent;

  @Column(name = "effective_from", nullable = true)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to", nullable = true)
  private LocalDate effectiveTo;
}

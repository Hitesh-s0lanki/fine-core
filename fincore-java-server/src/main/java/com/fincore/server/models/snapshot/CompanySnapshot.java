package com.fincore.server.models.snapshot;

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

/** Persistent company snapshot assertion. */
@Entity
@Table(name = "company_snapshot")
@DiscriminatorValue("snapshots")
@Getter
@Setter
@NoArgsConstructor
public class CompanySnapshot extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "cutoff_at", nullable = false)
  private Instant cutoffAt;

  @Column(name = "knowledge_basis", nullable = false)
  private String knowledgeBasis;

  @Column(name = "selection_policy_version", nullable = false)
  private String selectionPolicyVersion;

  @Column(name = "mapping_version", nullable = false)
  private String mappingVersion;

  @Column(name = "manifest_sha256", nullable = false)
  private String manifestSha256;

  @Column(name = "status", nullable = false)
  private String status;
}

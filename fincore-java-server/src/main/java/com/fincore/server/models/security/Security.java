package com.fincore.server.models.security;

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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent security assertion. */
@Entity
@Table(name = "security")
@DiscriminatorValue("securities")
@Getter
@Setter
@NoArgsConstructor
public class Security extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "security_type", nullable = false)
  private String securityType;

  @Column(name = "share_class", nullable = true)
  private String shareClass;

  @Column(name = "name", nullable = true)
  private String name;

  @Column(name = "is_fund", nullable = true)
  private Boolean isFund;

  @Column(name = "is_etf", nullable = true)
  private Boolean isEtf;
}

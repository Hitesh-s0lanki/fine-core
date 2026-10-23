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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent company registration assertion. */
@Entity
@Table(name = "company_registration")
@DiscriminatorValue("registrations")
@Getter
@Setter
@NoArgsConstructor
public class CompanyRegistration extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "registry", nullable = false)
  private String registry;

  @Column(name = "registry_jurisdiction", nullable = false)
  private String registryJurisdiction;

  @Column(name = "registration_number", nullable = false)
  private String registrationNumber;

  @Column(name = "registration_kind", nullable = false)
  private String registrationKind;

  @Column(name = "status_raw", nullable = true)
  private String statusRaw;

  @Column(name = "status_normalized", nullable = true)
  private String statusNormalized;

  @Column(name = "legal_form", nullable = true)
  private String legalForm;

  @Column(name = "incorporation_date", nullable = true)
  private LocalDate incorporationDate;

  @Column(name = "establishment_date", nullable = true)
  private LocalDate establishmentDate;

  @Column(name = "home_jurisdiction", nullable = true)
  private String homeJurisdiction;

  @Column(name = "parent_registry", nullable = true)
  private String parentRegistry;

  @Column(name = "governing_law", nullable = true)
  private String governingLaw;

  @Column(name = "objects", nullable = true)
  private String objects;

  @Column(name = "effective_from", nullable = true)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to", nullable = true)
  private LocalDate effectiveTo;
}

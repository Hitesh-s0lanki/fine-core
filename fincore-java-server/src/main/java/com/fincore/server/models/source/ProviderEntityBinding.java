package com.fincore.server.models.source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.security.Listing;
import com.fincore.server.models.security.Security;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent provider entity binding assertion. */
@Entity
@Table(name = "provider_entity_binding")
@DiscriminatorValue("provider-bindings")
@Getter
@Setter
@NoArgsConstructor
public class ProviderEntityBinding extends DomainRecord {
  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "company_id", nullable = true)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "security_id", nullable = true)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @Column(name = "listing_id", nullable = true)
  private UUID listingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Listing listingReference;

  @Column(name = "source_identifier", nullable = false)
  private String sourceIdentifier;

  @Column(name = "identifier_kind", nullable = false)
  private String identifierKind;

  @Column(name = "verification_status", nullable = false)
  private String verificationStatus;

  @Column(name = "verified_at", nullable = true)
  private Instant verifiedAt;

  @Column(name = "valid_from", nullable = true)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = true)
  private LocalDate validTo;
}

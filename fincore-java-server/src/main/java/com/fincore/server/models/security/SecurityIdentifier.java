package com.fincore.server.models.security;

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

/** Persistent security identifier assertion. */
@Entity
@Table(name = "security_identifier")
@DiscriminatorValue("security-identifiers")
@Getter
@Setter
@NoArgsConstructor
public class SecurityIdentifier extends DomainRecord {
  @Column(name = "security_id", nullable = false)
  private UUID securityId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Security securityReference;

  @Column(name = "scheme", nullable = false)
  private String scheme;

  @Column(name = "authority", nullable = false)
  private String authority;

  @Column(name = "value", nullable = false)
  private String value;

  @Column(name = "valid_from", nullable = true)
  private LocalDate validFrom;

  @Column(name = "valid_to", nullable = true)
  private LocalDate validTo;
}

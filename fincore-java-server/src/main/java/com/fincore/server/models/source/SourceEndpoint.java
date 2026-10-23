package com.fincore.server.models.source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent source endpoint assertion. */
@Entity
@Table(name = "source_endpoint")
@DiscriminatorValue("source-endpoints")
@Getter
@Setter
@NoArgsConstructor
public class SourceEndpoint extends DomainRecord {
  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "contract_version", nullable = false)
  private String contractVersion;

  @Column(name = "documentation_url", nullable = true)
  private String documentationUrl;

  @Column(name = "auth_scheme", nullable = false)
  private String authScheme;

  @Column(name = "credential_reference", nullable = true)
  private String credentialReference;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "supported_domains", nullable = false, columnDefinition = "jsonb")
  private Object supportedDomains;

  @Column(name = "pagination_kind", nullable = false)
  private String paginationKind;

  @Column(name = "access_status", nullable = false)
  private String accessStatus;

  @Column(name = "checked_at", nullable = false)
  private Instant checkedAt;
}

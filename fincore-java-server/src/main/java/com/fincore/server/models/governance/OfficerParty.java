package com.fincore.server.models.governance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.SourceProvider;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent officer party assertion. */
@Entity
@Table(name = "officer_party")
@DiscriminatorValue("officer-parties")
@Getter
@Setter
@NoArgsConstructor
public class OfficerParty extends DomainRecord {
  @Column(name = "party_kind", nullable = false)
  private String partyKind;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "source_officer_id", nullable = true)
  private String sourceOfficerId;

  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "identifiers", nullable = false, columnDefinition = "jsonb")
  private Object identifiers;
}

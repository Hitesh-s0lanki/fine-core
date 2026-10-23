package com.fincore.server.models.company;

import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent company assertion. */
@Entity
@Table(name = "company")
@DiscriminatorValue("companies")
@Getter
@Setter
@NoArgsConstructor
public class Company extends DomainRecord {
  @Column(name = "slug", nullable = false)
  private String slug;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "entity_kind", nullable = false)
  private String entityKind;
}

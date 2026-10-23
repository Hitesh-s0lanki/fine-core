package com.fincore.server.models.source;

import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent source provider assertion. */
@Entity
@Table(name = "source_provider")
@DiscriminatorValue("providers")
@Getter
@Setter
@NoArgsConstructor
public class SourceProvider extends DomainRecord {
  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "kind", nullable = false)
  private String kind;

  @Column(name = "base_url", nullable = true)
  private String baseUrl;
}

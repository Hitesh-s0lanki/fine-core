package com.fincore.server.models.common;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

/** Shared identity and immutable audit trail for canonical records. */
@org.hibernate.annotations.Immutable
@Entity
@Table(name = "data_record")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "record_type", length = 80)
@Getter
public abstract class DomainRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, updatable = false)
  private int schemaVersion = 1;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Setter
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "record_evidence", joinColumns = @JoinColumn(name = "record_id"))
  @Column(name = "evidence_id", nullable = false)
  @BatchSize(size = 100)
  private Set<UUID> evidenceIds = new LinkedHashSet<>();

  /** Initializes server-owned creation metadata. */
  @PrePersist
  protected void initializeAudit() {
    createdAt = Instant.now();
  }
}

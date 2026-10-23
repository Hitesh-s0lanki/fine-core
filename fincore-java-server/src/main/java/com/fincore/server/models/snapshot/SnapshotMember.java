package com.fincore.server.models.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
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

/** Persistent snapshot member assertion. */
@Entity
@Table(name = "snapshot_member")
@DiscriminatorValue("snapshot-members")
@Getter
@Setter
@NoArgsConstructor
public class SnapshotMember extends DomainRecord {
  @Column(name = "snapshot_id", nullable = false)
  private UUID snapshotId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private CompanySnapshot snapshotReference;

  @Column(name = "record_id", nullable = false)
  private UUID recordId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "record_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private DomainRecord recordReference;

  @Column(name = "role", nullable = false)
  private String role;

  @Column(name = "selection_reason", nullable = false)
  private String selectionReason;
}

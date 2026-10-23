package com.fincore.server.repository.snapshot;

import com.fincore.server.models.snapshot.SnapshotMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Typed persistence of frozen snapshot membership. */
public interface SnapshotMemberRepository
    extends JpaRepository<SnapshotMember, UUID>, JpaSpecificationExecutor<SnapshotMember> {
  /** Reads a frozen manifest in stable UUID order. */
  List<SnapshotMember> findBySnapshotIdOrderById(UUID snapshotId);

  /** Tests whether an immutable snapshot contains a specific source record. */
  boolean existsBySnapshotIdAndRecordId(UUID snapshotId, UUID recordId);
}

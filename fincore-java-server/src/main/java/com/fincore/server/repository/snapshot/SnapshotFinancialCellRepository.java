package com.fincore.server.repository.snapshot;

import com.fincore.server.models.snapshot.SnapshotFinancialCell;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Typed persistence of selected financial cells and explicit missing-value reasons. */
public interface SnapshotFinancialCellRepository
    extends JpaRepository<SnapshotFinancialCell, UUID>,
        JpaSpecificationExecutor<SnapshotFinancialCell> {
  /** Reads cells in their deterministic business-key order. */
  List<SnapshotFinancialCell> findBySnapshotIdOrderByCellKey(UUID snapshotId);
}

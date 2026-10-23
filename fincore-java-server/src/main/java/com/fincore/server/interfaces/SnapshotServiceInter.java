package com.fincore.server.interfaces;

import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.company.CompanyOverview;
import com.fincore.server.dto.snapshot.SnapshotRequest;
import java.util.UUID;

/** Application contract for immutable company snapshots and frozen overviews. */
public interface SnapshotServiceInter {
  /** Validates and persists an explicit snapshot selection manifest. */
  RecordView create(UUID companyId, SnapshotRequest request);

  /** Composes an overview from a previously frozen snapshot. */
  CompanyOverview overview(UUID companyId, UUID snapshotId);
}

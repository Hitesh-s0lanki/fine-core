package com.fincore.server.dto.company;

import com.fincore.server.dto.common.RecordView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Frozen overview composed exclusively of snapshot members and selected cells. */
public record CompanyOverview(
    UUID companyId,
    UUID snapshotId,
    Instant cutoffAt,
    RecordView snapshot,
    Map<String, List<RecordView>> sections,
    List<RecordView> financialCells) {}

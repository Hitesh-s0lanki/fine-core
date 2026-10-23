package com.fincore.server.dto.snapshot;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.snapshot.SnapshotMember;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for snapshot member. */
public record SnapshotMemberRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID snapshotId,
    @NotNull UUID recordId,
    @NotBlank @Size(max = 40000) String role,
    @NotBlank @Size(max = 40000) String selectionReason)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    SnapshotMember entity = new SnapshotMember();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSnapshotId(snapshotId);
    entity.setRecordId(recordId);
    entity.setRole(role);
    entity.setSelectionReason(selectionReason);
    return entity;
  }
}

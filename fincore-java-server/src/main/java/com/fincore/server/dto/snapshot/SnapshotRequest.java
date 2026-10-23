package com.fincore.server.dto.snapshot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Freezes caller-selected records; does not invent automatic source-precedence decisions. */
public record SnapshotRequest(
    @NotNull Instant cutoffAt,
    @NotBlank @Pattern(regexp = "collected_by_cutoff|published_by_cutoff|current_vintage")
        String knowledgeBasis,
    @NotBlank String selectionPolicyVersion,
    @NotBlank String mappingVersion,
    @NotEmpty @Size(max = 1000) List<@NotNull @Valid MemberSelection> members,
    @NotNull @Size(max = 1000) List<@NotNull @Valid CellSelection> financialCells) {}

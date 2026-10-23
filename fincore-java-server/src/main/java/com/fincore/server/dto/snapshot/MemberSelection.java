package com.fincore.server.dto.snapshot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** A selected, evidenced assertion and its reason for inclusion. */
public record MemberSelection(
    @NotNull UUID recordId, @NotBlank String role, @NotBlank String selectionReason) {}

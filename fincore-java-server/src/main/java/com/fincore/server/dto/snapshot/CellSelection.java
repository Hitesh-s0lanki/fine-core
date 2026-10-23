package com.fincore.server.dto.snapshot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** One statement cell; a missing cell supplies a reason instead of an observation. */
public record CellSelection(
    @NotNull UUID metricId,
    LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotBlank @Pattern(regexp = "consolidated|standalone|unspecified") String scope,
    @NotBlank String unit,
    UUID securityId,
    @NotNull List<Object> dimensions,
    UUID observationId,
    String missingReason) {}

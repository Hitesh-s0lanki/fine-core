package com.fincore.server.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** An identifier whose company is assigned by the onboarding service. */
public record InitialIdentifier(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Pattern(regexp = "cik|lei|cin|ein|provider_company_id|other") String scheme,
    @NotBlank @Size(max = 200) String authority,
    @NotBlank @Size(max = 200) String value,
    LocalDate validFrom,
    LocalDate validTo) {}

package com.fincore.server.dto.company;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Registers an issuer with at least one evidenced identifier in one transaction. */
public record CompanyOnboardingRequest(
    @NotNull @Valid CompanyRequest company,
    @NotEmpty @Size(max = 20) List<@NotNull @Valid InitialIdentifier> identifiers) {}

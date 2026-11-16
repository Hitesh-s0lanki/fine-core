package com.fincore.server.dto.ingestion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Contracts for single-response profile and explicitly selected raw SEC fact ingestion. */
public final class IngestionContracts {
  private IngestionContracts() {}

  /** Operator-configured source, verified identity and explicitly pinned adapter. */
  public record Target(
      @NotNull UUID bindingId,
      @NotNull UUID endpointId,
      @NotBlank @Size(max = 2000) String fetchUrl,
      @NotBlank @Pattern(regexp = "[a-z][a-z0-9_-]{0,63}") String sourceId,
      @NotNull @Pattern(regexp = "[a-f0-9]{64}") String adapterId,
      @NotBlank @Size(max = 200) String registry,
      @NotBlank @Size(max = 200) String registryJurisdiction,
      @NotNull @Pattern(regexp = "domestic|overseas|branch|unknown") String registrationKind,
      @Min(60) Integer refreshSeconds,
      @Pattern(regexp = "company_profile|sec_company_facts") String operation,
      @Valid SecFinancialContracts.Selection selection) {
    /** Existing profile targets retain their original operation when the field is absent. */
    public Target {
      if (operation == null) {
        operation = "company_profile";
      }
    }
  }

  /** Original HTTP response metadata; the worker retains the bytes separately. */
  public record Artifact(
      @NotBlank @Size(max = 4000) String storageUri,
      @NotNull @Pattern(regexp = "[a-f0-9]{64}") String sha256,
      @Min(0) long byteLength,
      @NotBlank @Size(max = 200) String contentType,
      @NotNull Instant storedAt) {}

  /** Explicitly supported normalized record. Financial records need a richer bridge. */
  public record Profile(
      @NotNull @Pattern(regexp = "company_profile") String recordType,
      @NotBlank @Size(max = 40000) String name,
      @NotBlank @Size(max = 40000) String registrationNumber,
      @Size(max = 40000) String status,
      @Size(max = 40000) String jurisdiction) {}

  /** One successful response and its normalized profile, committed atomically with a snapshot. */
  public record Completion(
      @NotNull UUID leaseToken,
      @NotNull Instant requestedAt,
      @NotNull Instant retrievedAt,
      @NotNull @Valid Artifact artifact,
      @NotNull @Pattern(regexp = "[a-f0-9]{64}") String adapterId,
      @NotBlank String sourceId,
      @NotNull @Size(min = 1, max = 1) List<@NotNull @Valid Profile> records) {}

  /** Failed work; only transient source failures are eligible for bounded retries. */
  public record Failure(
      @NotNull UUID leaseToken,
      @NotNull
          @Pattern(
              regexp =
                  "timeout|network|rate_limited|provider_error|invalid_payload|normalization|"
                      + "unsupported_record|configuration")
          String errorCode,
      @NotNull Instant requestedAt,
      @NotNull Instant retrievedAt,
      @Min(100) @Max(599) Integer httpStatus,
      @Valid Artifact artifact) {}
}

package com.fincore.server.dto.discovery;

import com.fincore.server.dto.common.RecordView;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Public contracts for SEC directory discovery and explicit company import. */
public final class DiscoveryContracts {
  private DiscoveryContracts() {}

  /** One SEC directory association; a ticker is not a verified exchange listing. */
  public record Match(String cik, String ticker, String name, UUID companyId) {}

  /** Bounded directory results with source retrieval time, not publication time. */
  public record Results(
      List<Match> items,
      int page,
      int size,
      long totalElements,
      boolean hasNext,
      String sourceUrl,
      Instant retrievedAt) {}

  /** Imports only a ten-digit CIK verified against the current SEC directory. */
  public record ImportRequest(@NotNull @Pattern(regexp = "(?!0000000000)[0-9]{10}") String cik) {}

  /** Identity-only import result; financial ingestion remains a separate workflow. */
  public record Imported(RecordView company, boolean created, String coverage) {}
}

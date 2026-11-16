package com.fincore.server.dto.ingestion;

import com.fincore.server.dto.ingestion.IngestionContracts.Artifact;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Versioned, bounded contracts for selected raw SEC Company Facts observations. */
public final class SecFinancialContracts {
  public static final String VERSION = "sec-companyfacts-v1";
  public static final String SOURCE = "sec_company_facts";
  public static final String PARSER_ID =
      "dd4a7be0cc621267de043188f8bd2f7863164e14ade7a91b2644125d3e2fdaa6";

  private SecFinancialContracts() {}

  /** Explicit raw concepts; this is neither canonical metric mapping nor annual selection. */
  public record Selection(
      @NotNull @Pattern(regexp = "us-gaap|ifrs-full|dei|srt") String taxonomy,
      @NotNull @Size(min = 1, max = 20)
          List<@NotNull @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_.-]{0,199}") String> concepts) {}

  /** Requests a manual-only refresh for an already evidenced SEC CIK company. */
  public record Refresh(@NotNull UUID companyId, @NotNull @Valid Selection selection) {}

  /** One source array element, preserving native period, accession, unit and numeric token. */
  public record Fact(
      @NotNull @Pattern(regexp = "us-gaap|ifrs-full|dei|srt") String taxonomy,
      @NotNull @Pattern(regexp = "[A-Za-z_][A-Za-z0-9_.-]{0,199}") String concept,
      @Size(max = 40000) String label,
      @Size(max = 40000) String description,
      @NotBlank @Size(max = 200) String unit,
      @NotNull @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
      @NotNull @Size(max = 200) @Pattern(regexp = "-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
          String reportedValueRaw,
      LocalDate periodStart,
      @NotNull LocalDate periodEnd,
      Integer fiscalYear,
      @Size(max = 20) String fiscalPeriod,
      @Size(max = 100) String frame,
      @NotNull @Pattern(regexp = "[0-9]{10}-[0-9]{2}-[0-9]{6}") String accession,
      @NotBlank @Size(max = 40) String form,
      @NotNull LocalDate filedOn,
      @NotBlank @Size(max = 1000) String locator) {}

  /** Atomic completion of one explicitly selected raw-fact response, without a snapshot. */
  public record Completion(
      @NotNull UUID leaseToken,
      @NotNull Instant requestedAt,
      @NotNull Instant retrievedAt,
      @NotNull @Valid Artifact artifact,
      @NotNull @Pattern(regexp = "[a-f0-9]{64}") String adapterId,
      @NotBlank String sourceId,
      @NotNull @Pattern(regexp = "(?!0000000000)[0-9]{10}") String cik,
      @NotNull @Size(min = 1, max = 5000) List<@NotNull @Valid Fact> records) {}
}

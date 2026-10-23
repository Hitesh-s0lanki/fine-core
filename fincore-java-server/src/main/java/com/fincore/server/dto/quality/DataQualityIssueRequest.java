package com.fincore.server.dto.quality;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.quality.DataQualityIssue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for data quality issue. */
public record DataQualityIssueRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    UUID companyId,
    @NotBlank @Size(max = 40000) String recordType,
    UUID recordId,
    @NotBlank @Size(max = 40000) String code,
    @NotNull @Pattern(regexp = "info|warning|error") String severity,
    @NotBlank @Size(max = 40000) String message,
    @NotNull Instant detectedAt,
    @NotBlank @Size(max = 40000) String ruleVersion,
    @Size(max = 40000) String resolution)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    DataQualityIssue entity = new DataQualityIssue();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setRecordType(recordType);
    entity.setRecordId(recordId);
    entity.setCode(code);
    entity.setSeverity(severity);
    entity.setMessage(message);
    entity.setDetectedAt(detectedAt);
    entity.setRuleVersion(ruleVersion);
    entity.setResolution(resolution);
    return entity;
  }
}

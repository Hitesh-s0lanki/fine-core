package com.fincore.server.dto.reference;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.reference.ReferenceDatasetObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for reference dataset observation. */
public record ReferenceDatasetObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID datasetId,
    @Size(max = 40000) String geography,
    @Size(max = 40000) String industryScheme,
    @Size(max = 40000) String industryLabel,
    @NotBlank @Size(max = 40000) String metricCode,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
    @NotBlank @Size(max = 40000) String unit,
    Integer sampleSize,
    @Size(max = 40000) String methodologyNotes,
    LocalDate asOfDate)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    ReferenceDatasetObservation entity = new ReferenceDatasetObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setDatasetId(datasetId);
    entity.setGeography(geography);
    entity.setIndustryScheme(industryScheme);
    entity.setIndustryLabel(industryLabel);
    entity.setMetricCode(metricCode);
    entity.setValue(Decimals.parse(value));
    entity.setUnit(unit);
    entity.setSampleSize(sampleSize);
    entity.setMethodologyNotes(methodologyNotes);
    entity.setAsOfDate(asOfDate);
    return entity;
  }
}

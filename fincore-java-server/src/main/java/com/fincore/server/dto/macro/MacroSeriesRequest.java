package com.fincore.server.dto.macro;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.macro.MacroSeries;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for macro series. */
public record MacroSeriesRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID providerId,
    @NotBlank @Size(max = 40000) String sourceSeriesId,
    @NotBlank @Size(max = 40000) String name,
    @NotBlank @Size(max = 40000) String geography,
    @NotBlank @Size(max = 40000) String unit,
    @NotBlank @Size(max = 40000) String frequency,
    @Size(max = 40000) String seasonalAdjustment,
    @NotBlank @Size(max = 40000) String transformation,
    Integer unitMultiplier,
    @Size(max = 40000) String notes,
    Instant lastUpdatedAt)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    MacroSeries entity = new MacroSeries();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setProviderId(providerId);
    entity.setSourceSeriesId(sourceSeriesId);
    entity.setName(name);
    entity.setGeography(geography);
    entity.setUnit(unit);
    entity.setFrequency(frequency);
    entity.setSeasonalAdjustment(seasonalAdjustment);
    entity.setTransformation(transformation);
    entity.setUnitMultiplier(unitMultiplier);
    entity.setNotes(notes);
    entity.setLastUpdatedAt(lastUpdatedAt);
    return entity;
  }
}

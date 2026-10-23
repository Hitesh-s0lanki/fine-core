package com.fincore.server.dto.macro;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.macro.MacroObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for macro observation. */
public record MacroObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID seriesId,
    @NotNull LocalDate observationDate,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
    @Size(max = 40000) String missingReason,
    LocalDate vintageDate,
    @NotNull @Pattern(regexp = "current_download|historical_vintage") String vintageBasis,
    Instant releasedAt,
    LocalDate realtimeStart,
    LocalDate realtimeEnd)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    MacroObservation entity = new MacroObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSeriesId(seriesId);
    entity.setObservationDate(observationDate);
    entity.setValue(Decimals.parse(value));
    entity.setMissingReason(missingReason);
    entity.setVintageDate(vintageDate);
    entity.setVintageBasis(vintageBasis);
    entity.setReleasedAt(releasedAt);
    entity.setRealtimeStart(realtimeStart);
    entity.setRealtimeEnd(realtimeEnd);
    return entity;
  }
}

package com.fincore.server.dto.market;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.market.FxObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for fx observation. */
public record FxObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String baseCurrency,
    @NotBlank @Size(max = 40000) String quoteCurrency,
    @NotNull Instant observedAt,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String rate,
    @NotNull @Pattern(regexp = "spot|period_average|period_end") String rateKind,
    LocalDate periodStart,
    LocalDate periodEnd)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    FxObservation entity = new FxObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setBaseCurrency(baseCurrency);
    entity.setQuoteCurrency(quoteCurrency);
    entity.setObservedAt(observedAt);
    entity.setRate(Decimals.parse(rate));
    entity.setRateKind(rateKind);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    return entity;
  }
}

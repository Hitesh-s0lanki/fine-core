package com.fincore.server.dto.market;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.market.PriceBar;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for price bar. */
public record PriceBarRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID listingId,
    @NotNull @Pattern(regexp = "daily|minute|hour|week|month") String interval,
    LocalDate tradingDate,
    @NotBlank @Size(max = 40000) String exchangeTimezone,
    Instant periodStart,
    Instant periodEnd,
    @NotBlank @Size(max = 40000) String currency,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String open,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String high,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String low,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String close,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String volume,
    @NotNull @Pattern(regexp = "raw|split|total_return|unknown") String adjustmentBasis,
    @Size(max = 40000) String adjustmentVersion,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String adjustedClose,
    @Size(max = 40000) String adjustedCloseBasis)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    PriceBar entity = new PriceBar();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setListingId(listingId);
    entity.setInterval(interval);
    entity.setTradingDate(tradingDate);
    entity.setExchangeTimezone(exchangeTimezone);
    entity.setPeriodStart(periodStart);
    entity.setPeriodEnd(periodEnd);
    entity.setCurrency(currency);
    entity.setOpen(Decimals.parse(open));
    entity.setHigh(Decimals.parse(high));
    entity.setLow(Decimals.parse(low));
    entity.setClose(Decimals.parse(close));
    entity.setVolume(Decimals.parse(volume));
    entity.setAdjustmentBasis(adjustmentBasis);
    entity.setAdjustmentVersion(adjustmentVersion);
    entity.setAdjustedClose(Decimals.parse(adjustedClose));
    entity.setAdjustedCloseBasis(adjustedCloseBasis);
    return entity;
  }
}

package com.fincore.server.dto.market;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.market.MarketQuote;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for market quote. */
public record MarketQuoteRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID listingId,
    @NotNull Instant asOf,
    @NotBlank @Size(max = 40000) String currency,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String lastPrice,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String bid,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String ask,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String volume,
    @Size(max = 40000) String marketStatus,
    Integer delaySeconds)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    MarketQuote entity = new MarketQuote();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setListingId(listingId);
    entity.setAsOf(asOf);
    entity.setCurrency(currency);
    entity.setLastPrice(Decimals.parse(lastPrice));
    entity.setBid(Decimals.parse(bid));
    entity.setAsk(Decimals.parse(ask));
    entity.setVolume(Decimals.parse(volume));
    entity.setMarketStatus(marketStatus);
    entity.setDelaySeconds(delaySeconds);
    return entity;
  }
}

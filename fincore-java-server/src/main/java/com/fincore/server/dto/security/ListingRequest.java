package com.fincore.server.dto.security;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.security.Listing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for listing. */
public record ListingRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID securityId,
    @NotBlank @Size(max = 40000) String exchangeCode,
    @Size(max = 40000) String exchangeMic,
    @NotBlank @Size(max = 40000) String symbol,
    @Size(max = 40000) String tradingCurrency,
    @NotNull @Pattern(regexp = "active|inactive|delisted|unknown") String status,
    LocalDate firstTradeDate,
    LocalDate validFrom,
    LocalDate validTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    Listing entity = new Listing();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSecurityId(securityId);
    entity.setExchangeCode(exchangeCode);
    entity.setExchangeMic(exchangeMic);
    entity.setSymbol(symbol);
    entity.setTradingCurrency(tradingCurrency);
    entity.setStatus(status);
    entity.setFirstTradeDate(firstTradeDate);
    entity.setValidFrom(validFrom);
    entity.setValidTo(validTo);
    return entity;
  }
}

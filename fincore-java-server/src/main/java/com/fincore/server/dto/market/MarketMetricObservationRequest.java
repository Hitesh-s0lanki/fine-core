package com.fincore.server.dto.market;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.market.MarketMetricObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for market metric observation. */
public record MarketMetricObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    UUID securityId,
    UUID listingId,
    @NotNull
        @Pattern(
            regexp =
                "MARKET_CAP|ENTERPRISE_VALUE|BETA|AVERAGE_VOLUME|"
                    + "DIVIDEND_PER_SHARE|DIVIDEND_YIELD|PRICE_CHANGE|"
                    + "PRICE_CHANGE_PERCENT|PRICE_RANGE_LOW|PRICE_RANGE_HIGH|"
                    + "OTHER")
        String metricCode,
    @NotBlank @Size(max = 40000) String metricDefinition,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String value,
    @NotBlank @Size(max = 40000) String unit,
    @Size(max = 40000) String currency,
    Instant asOf,
    LocalDate referencePeriodStart,
    LocalDate referencePeriodEnd,
    @Size(max = 40000) String methodology,
    @NotNull Object inputRecordRefs)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    MarketMetricObservation entity = new MarketMetricObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setSecurityId(securityId);
    entity.setListingId(listingId);
    entity.setMetricCode(metricCode);
    entity.setMetricDefinition(metricDefinition);
    entity.setValue(Decimals.parse(value));
    entity.setUnit(unit);
    entity.setCurrency(currency);
    entity.setAsOf(asOf);
    entity.setReferencePeriodStart(referencePeriodStart);
    entity.setReferencePeriodEnd(referencePeriodEnd);
    entity.setMethodology(methodology);
    entity.setInputRecordRefs(inputRecordRefs);
    return entity;
  }
}

package com.fincore.server.dto.market;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.market.CorporateAction;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for corporate action. */
public record CorporateActionRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID securityId,
    @NotNull @Pattern(regexp = "split|dividend|buyback|rights|other") String actionType,
    Instant announcedAt,
    LocalDate exDate,
    LocalDate recordDate,
    LocalDate paymentDate,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String amount,
    @Size(max = 40000) String currency,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String ratioNumerator,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String ratioDenominator)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CorporateAction entity = new CorporateAction();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSecurityId(securityId);
    entity.setActionType(actionType);
    entity.setAnnouncedAt(announcedAt);
    entity.setExDate(exDate);
    entity.setRecordDate(recordDate);
    entity.setPaymentDate(paymentDate);
    entity.setAmount(Decimals.parse(amount));
    entity.setCurrency(currency);
    entity.setRatioNumerator(Decimals.parse(ratioNumerator));
    entity.setRatioDenominator(Decimals.parse(ratioDenominator));
    return entity;
  }
}

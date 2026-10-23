package com.fincore.server.dto.security;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.security.DepositaryReceiptRatio;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for depositary receipt ratio. */
public record DepositaryReceiptRatioRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID receiptSecurityId,
    @NotNull UUID underlyingSecurityId,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String receiptUnits,
    @NotBlank @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String underlyingUnits,
    @NotNull LocalDate effectiveFrom,
    LocalDate effectiveTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    DepositaryReceiptRatio entity = new DepositaryReceiptRatio();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setReceiptSecurityId(receiptSecurityId);
    entity.setUnderlyingSecurityId(underlyingSecurityId);
    entity.setReceiptUnits(Decimals.parse(receiptUnits));
    entity.setUnderlyingUnits(Decimals.parse(underlyingUnits));
    entity.setEffectiveFrom(effectiveFrom);
    entity.setEffectiveTo(effectiveTo);
    return entity;
  }
}

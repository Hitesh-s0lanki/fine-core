package com.fincore.server.dto.governance;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.governance.OwnershipObservation;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for ownership observation. */
public record OwnershipObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    UUID securityId,
    @NotBlank @Size(max = 40000) String holderName,
    UUID holderCompanyId,
    @NotNull LocalDate asOfDate,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String shares,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String ownershipPercent,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String votingPercent)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    OwnershipObservation entity = new OwnershipObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setSecurityId(securityId);
    entity.setHolderName(holderName);
    entity.setHolderCompanyId(holderCompanyId);
    entity.setAsOfDate(asOfDate);
    entity.setShares(Decimals.parse(shares));
    entity.setOwnershipPercent(Decimals.parse(ownershipPercent));
    entity.setVotingPercent(Decimals.parse(votingPercent));
    return entity;
  }
}

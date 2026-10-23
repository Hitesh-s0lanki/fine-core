package com.fincore.server.dto.company;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.CompanyRelationship;
import com.fincore.server.util.Decimals;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for company relationship. */
public record CompanyRelationshipRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID subjectCompanyId,
    @NotNull UUID objectCompanyId,
    @NotNull @Pattern(regexp = "subsidiary|associate|joint_venture|parent|other")
        String relationshipType,
    @Pattern(regexp = "-?(0|[1-9][0-9]{0,41})(\\.[0-9]{1,18})?") String ownershipPercent,
    LocalDate effectiveFrom,
    LocalDate effectiveTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CompanyRelationship entity = new CompanyRelationship();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setSubjectCompanyId(subjectCompanyId);
    entity.setObjectCompanyId(objectCompanyId);
    entity.setRelationshipType(relationshipType);
    entity.setOwnershipPercent(Decimals.parse(ownershipPercent));
    entity.setEffectiveFrom(effectiveFrom);
    entity.setEffectiveTo(effectiveTo);
    return entity;
  }
}

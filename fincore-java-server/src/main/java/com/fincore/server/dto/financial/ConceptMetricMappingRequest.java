package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.ConceptMetricMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for concept metric mapping. */
public record ConceptMetricMappingRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID conceptId,
    @NotNull UUID metricId,
    UUID companyId,
    @Pattern(regexp = "consolidated|standalone|unspecified") String scopeConstraint,
    @Size(max = 40000) String accountingStandard,
    @NotBlank @Size(max = 40000) String mappingVersion,
    @NotBlank @Size(max = 40000) String semanticNotes,
    @Size(max = 40000) String transformMethod,
    LocalDate validFrom,
    LocalDate validTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    ConceptMetricMapping entity = new ConceptMetricMapping();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setConceptId(conceptId);
    entity.setMetricId(metricId);
    entity.setCompanyId(companyId);
    entity.setScopeConstraint(scopeConstraint);
    entity.setAccountingStandard(accountingStandard);
    entity.setMappingVersion(mappingVersion);
    entity.setSemanticNotes(semanticNotes);
    entity.setTransformMethod(transformMethod);
    entity.setValidFrom(validFrom);
    entity.setValidTo(validTo);
    return entity;
  }
}

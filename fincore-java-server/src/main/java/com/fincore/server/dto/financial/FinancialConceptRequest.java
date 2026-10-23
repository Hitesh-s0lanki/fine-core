package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.FinancialConcept;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for financial concept. */
public record FinancialConceptRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String taxonomy,
    @Size(max = 40000) String taxonomyVersion,
    @NotBlank @Size(max = 40000) String conceptName,
    @Size(max = 40000) String label,
    @Size(max = 40000) String description,
    @NotNull @Pattern(regexp = "instant|duration|unknown") String periodType)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    FinancialConcept entity = new FinancialConcept();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setTaxonomy(taxonomy);
    entity.setTaxonomyVersion(taxonomyVersion);
    entity.setConceptName(conceptName);
    entity.setLabel(label);
    entity.setDescription(description);
    entity.setPeriodType(periodType);
    return entity;
  }
}

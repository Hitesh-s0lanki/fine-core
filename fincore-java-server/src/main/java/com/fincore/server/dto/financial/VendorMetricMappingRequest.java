package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.VendorMetricMapping;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for vendor metric mapping. */
public record VendorMetricMappingRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID providerId,
    @NotNull UUID sourceEndpointId,
    @NotBlank @Size(max = 40000) String sourceField,
    @NotNull UUID metricId,
    @NotBlank @Size(max = 40000) String mappingVersion,
    @NotNull @Pattern(regexp = "verified|ambiguous") String definitionStatus,
    @NotBlank @Size(max = 40000) String semanticNotes,
    @Size(max = 40000) String transformMethod)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    VendorMetricMapping entity = new VendorMetricMapping();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setProviderId(providerId);
    entity.setSourceEndpointId(sourceEndpointId);
    entity.setSourceField(sourceField);
    entity.setMetricId(metricId);
    entity.setMappingVersion(mappingVersion);
    entity.setDefinitionStatus(definitionStatus);
    entity.setSemanticNotes(semanticNotes);
    entity.setTransformMethod(transformMethod);
    return entity;
  }
}

package com.fincore.server.dto.financial;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.financial.MetricDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for metric definition. */
public record MetricDefinitionRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String code,
    @NotBlank @Size(max = 40000) String label,
    @NotNull @Pattern(regexp = "income|balance|cash_flow|per_share|other") String statement,
    @NotNull @Pattern(regexp = "instant|duration|unknown") String periodType,
    @NotBlank @Size(max = 40000) String unitKind,
    @NotBlank @Size(max = 40000) String definition,
    @NotBlank @Size(max = 40000) String signConvention)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    MetricDefinition entity = new MetricDefinition();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCode(code);
    entity.setLabel(label);
    entity.setStatement(statement);
    entity.setPeriodType(periodType);
    entity.setUnitKind(unitKind);
    entity.setDefinition(definition);
    entity.setSignConvention(signConvention);
    return entity;
  }
}

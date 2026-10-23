package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.SourceProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for source provider. */
public record SourceProviderRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotBlank @Size(max = 40000) String code,
    @NotBlank @Size(max = 40000) String name,
    @NotNull @Pattern(regexp = "regulator|exchange|vendor|macro|manual") String kind,
    @Size(max = 40000) String baseUrl)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    SourceProvider entity = new SourceProvider();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCode(code);
    entity.setName(name);
    entity.setKind(kind);
    entity.setBaseUrl(baseUrl);
    return entity;
  }
}

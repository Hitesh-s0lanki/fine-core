package com.fincore.server.dto.source;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.IngestionAttempt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for ingestion attempt. */
public record IngestionAttemptRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID providerId,
    @NotNull UUID sourceEndpointId,
    UUID providerEntityBindingId,
    UUID companyId,
    @NotBlank @Size(max = 40000) String endpoint,
    @NotBlank @Size(max = 40000) String requestUrlRedacted,
    @NotNull Instant requestedAt,
    Instant retrievedAt,
    Integer httpStatus,
    @NotNull
        @Pattern(
            regexp =
                "succeeded|authentication_required|forbidden|rate_limited|failed|not_attempted")
        String outcome,
    @Size(max = 40000) String errorCode,
    @Size(max = 40000) String errorMessage,
    UUID artifactId,
    @NotNull Object requestScope,
    @Size(max = 40000) String nextCursor,
    Boolean paginationComplete)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    IngestionAttempt entity = new IngestionAttempt();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setProviderId(providerId);
    entity.setSourceEndpointId(sourceEndpointId);
    entity.setProviderEntityBindingId(providerEntityBindingId);
    entity.setCompanyId(companyId);
    entity.setEndpoint(endpoint);
    entity.setRequestUrlRedacted(requestUrlRedacted);
    entity.setRequestedAt(requestedAt);
    entity.setRetrievedAt(retrievedAt);
    entity.setHttpStatus(httpStatus);
    entity.setOutcome(outcome);
    entity.setErrorCode(errorCode);
    entity.setErrorMessage(errorMessage);
    entity.setArtifactId(artifactId);
    entity.setRequestScope(requestScope);
    entity.setNextCursor(nextCursor);
    entity.setPaginationComplete(paginationComplete);
    return entity;
  }
}

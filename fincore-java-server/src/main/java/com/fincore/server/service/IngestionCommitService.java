package com.fincore.server.service;

import com.fincore.server.dto.company.CompanyProfileObservationRequest;
import com.fincore.server.dto.company.CompanyRegistrationRequest;
import com.fincore.server.dto.ingestion.IngestionContracts.Artifact;
import com.fincore.server.dto.ingestion.IngestionContracts.Completion;
import com.fincore.server.dto.ingestion.IngestionContracts.Failure;
import com.fincore.server.dto.ingestion.IngestionContracts.Target;
import com.fincore.server.dto.snapshot.MemberSelection;
import com.fincore.server.dto.snapshot.SnapshotRequest;
import com.fincore.server.dto.source.IngestionAttemptRequest;
import com.fincore.server.dto.source.NormalizationRunRequest;
import com.fincore.server.dto.source.RawArtifactRequest;
import com.fincore.server.dto.source.SourceReferenceRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.interfaces.SnapshotServiceInter;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.source.ProviderEntityBinding;
import com.fincore.server.models.source.SourceEndpoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically bridges normalized profiles into immutable evidence, assertions and snapshots. */
@Service
@Transactional
public class IngestionCommitService {
  private final IngestionJobService jobs;
  private final CompanyDataServiceInter data;
  private final SnapshotServiceInter snapshots;

  /** Shares the job transaction with all canonical record and snapshot writes. */
  public IngestionCommitService(
      IngestionJobService jobs, CompanyDataServiceInter data, SnapshotServiceInter snapshots) {
    this.jobs = jobs;
    this.data = data;
    this.snapshots = snapshots;
  }

  /**
   * Commits exactly one supported profile; repeated acknowledgement returns the original result.
   */
  public Map<String, Object> complete(UUID id, Completion input) {
    var job = jobs.lock(id, input.leaseToken());
    if ("succeeded".equals(job.get("status"))) {
      return job;
    }
    Target target = jobs.target((UUID) job.get("targetId"));
    if (!"company_profile".equals(target.operation())) {
      throw new IllegalArgumentException("Use the financial completion endpoint for this target");
    }
    ProviderEntityBinding binding = jobs.binding(target);
    if (!target.adapterId().equals(input.adapterId())
        || !target.sourceId().equals(input.sourceId())
        || !binding.getSourceIdentifier().equals(input.records().getFirst().registrationNumber())) {
      throw new IllegalArgumentException("Adapter, source or company identity mismatch");
    }
    checkTimes(input.requestedAt(), input.retrievedAt());
    UUID artifact = artifact(input.artifact(), "json");
    UUID attempt =
        attempt(target, binding, input.requestedAt(), input.retrievedAt(), 200, null, artifact);
    // Create source evidence before the output assertions; the final run links all output IDs.
    UUID evidence =
        save(
                "source-references",
                new SourceReferenceRequest(
                    Set.of(),
                    attempt,
                    artifact,
                    null,
                    target.fetchUrl(),
                    "$",
                    "api",
                    target.adapterId(),
                    input.retrievedAt(),
                    null,
                    "unknown",
                    null))
            .getId();
    var profile = input.records().getFirst();
    List<UUID> outputs = new ArrayList<>();
    outputs.add(
        save(
                "profile-observations",
                new CompanyProfileObservationRequest(
                    Set.of(evidence),
                    binding.getCompanyId(),
                    "legal_name",
                    profile.name(),
                    null,
                    null))
            .getId());
    outputs.add(
        save(
                "registrations",
                new CompanyRegistrationRequest(
                    Set.of(evidence),
                    binding.getCompanyId(),
                    target.registry(),
                    target.registryJurisdiction(),
                    profile.registrationNumber(),
                    target.registrationKind(),
                    profile.status(),
                    null,
                    null,
                    null,
                    null,
                    profile.jurisdiction(),
                    null,
                    null,
                    null,
                    null,
                    null))
            .getId());
    UUID run =
        save(
                "normalization-runs",
                new NormalizationRunRequest(
                    Set.of(),
                    attempt,
                    target.adapterId(),
                    target.adapterId(),
                    target.adapterId(),
                    1,
                    "profile-v1",
                    input.retrievedAt(),
                    Instant.now(),
                    "succeeded",
                    outputs,
                    List.of()))
            .getId();
    List<MemberSelection> members = new ArrayList<>();
    members.add(
        new MemberSelection(binding.getCompanyId(), "identity", "Verified provider binding"));
    members.add(new MemberSelection(attempt, "ingestion", "Completed single-response JSON fetch"));
    members.add(new MemberSelection(run, "normalization", "Pinned certified adapter"));
    for (UUID output : outputs) {
      members.add(
          new MemberSelection(
              output, "profile", "Profile response for this refresh; no financial selection"));
    }
    UUID snapshot =
        snapshots
            .create(
                binding.getCompanyId(),
                new SnapshotRequest(
                    Instant.now(),
                    "current_vintage",
                    "profile-only-v1",
                    "profile-v1",
                    members,
                    List.of()))
            .id();
    jobs.succeed(id, snapshot);
    return jobs.get(id);
  }

  /** Records failed work as a new attempt and applies the operational retry policy. */
  public Map<String, Object> fail(UUID id, Failure input) {
    var job = jobs.lock(id, input.leaseToken());
    if ("succeeded".equals(job.get("status"))) {
      return job;
    }
    checkTimes(input.requestedAt(), input.retrievedAt());
    Target target = jobs.target((UUID) job.get("targetId"));
    UUID artifact = input.artifact() == null ? null : artifact(input.artifact(), "text");
    UUID attempt =
        attempt(
            target,
            jobs.binding(target),
            input.requestedAt(),
            input.retrievedAt(),
            input.httpStatus(),
            input.errorCode(),
            artifact);
    if (List.of("normalization", "unsupported_record").contains(input.errorCode())) {
      save(
          "normalization-runs",
          new NormalizationRunRequest(
              Set.of(),
              attempt,
              target.adapterId(),
              target.adapterId(),
              target.adapterId(),
              1,
              "sec_company_facts".equals(target.operation()) ? "sec-companyfacts-v1" : "profile-v1",
              input.retrievedAt(),
              Instant.now(),
              "failed",
              List.of(),
              List.of()));
    }
    jobs.fail(id, input.errorCode());
    return jobs.get(id);
  }

  private void checkTimes(Instant requested, Instant retrieved) {
    if (requested.isAfter(retrieved) || retrieved.isAfter(Instant.now().plusSeconds(30))) {
      throw new IllegalArgumentException("Invalid fetch timestamps");
    }
  }

  private UUID artifact(Artifact input, String format) {
    return save(
            "raw-artifacts",
            new RawArtifactRequest(
                Set.of(),
                input.storageUri(),
                input.sha256(),
                "response_bytes",
                input.contentType(),
                input.byteLength(),
                format,
                input.storedAt(),
                input.sha256()))
        .getId();
  }

  private UUID attempt(
      Target target,
      ProviderEntityBinding binding,
      Instant requested,
      Instant retrieved,
      Integer status,
      String error,
      UUID artifact) {
    SourceEndpoint endpoint = (SourceEndpoint) data.load("source-endpoints", target.endpointId());
    String outcome = error == null || (status != null && status == 200) ? "succeeded" : "failed";
    if (status != null && status == 429) {
      outcome = "rate_limited";
    }
    return save(
            "ingestion-attempts",
            new IngestionAttemptRequest(
                Set.of(),
                binding.getProviderId(),
                target.endpointId(),
                target.bindingId(),
                binding.getCompanyId(),
                endpoint.getCode(),
                target.fetchUrl(),
                requested,
                retrieved,
                status,
                outcome,
                error,
                null,
                artifact,
                Map.of("sourceId", target.sourceId()),
                null,
                "succeeded".equals(outcome)))
        .getId();
  }

  private DomainRecord save(String resource, CreationRequest request) {
    return data.save(data.resource(resource), request);
  }
}

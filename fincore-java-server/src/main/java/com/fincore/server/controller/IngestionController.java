package com.fincore.server.controller;

import com.fincore.server.dto.ingestion.IngestionContracts.Completion;
import com.fincore.server.dto.ingestion.IngestionContracts.Failure;
import com.fincore.server.dto.ingestion.IngestionContracts.Target;
import com.fincore.server.service.IngestionCommitService;
import com.fincore.server.service.IngestionJobService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Local trusted-worker API for configuring, dispatching and acknowledging ingestion. */
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {
  private final IngestionJobService jobs;
  private final IngestionCommitService commits;

  /** Coordinates durable jobs and atomic domain commits. */
  public IngestionController(IngestionJobService jobs, IngestionCommitService commits) {
    this.jobs = jobs;
    this.commits = commits;
  }

  /** Creates an operator-configured target. */
  @PostMapping("/targets")
  public Map<String, Object> target(@Valid @RequestBody Target target) {
    return jobs.createTarget(target);
  }

  /** Requests a refresh, or returns the existing unfinished job. */
  @PostMapping("/targets/{id}/refresh")
  public Map<String, Object> refresh(@PathVariable UUID id) {
    return jobs.enqueue(id);
  }

  /** Reads the current job status. */
  @GetMapping("/jobs/{id}")
  public Map<String, Object> get(@PathVariable UUID id) {
    return jobs.get(id);
  }

  /** Claims work with a five-minute lease; returns 204 when the queue has no ready work. */
  @PostMapping("/jobs/claim")
  public ResponseEntity<Map<String, Object>> claim(@RequestParam(required = false) UUID jobId) {
    var work = jobs.claim(jobId);
    return work.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(work);
  }

  /** Atomically commits a supported result and frozen profile overview. */
  @PostMapping("/jobs/{id}/complete")
  public Map<String, Object> complete(@PathVariable UUID id, @Valid @RequestBody Completion input) {
    return commits.complete(id, input);
  }

  /** Records failed work and schedules transient retries. */
  @PostMapping("/jobs/{id}/fail")
  public Map<String, Object> fail(@PathVariable UUID id, @Valid @RequestBody Failure input) {
    return commits.fail(id, input);
  }
}

package com.fincore.server.config;

import com.fincore.server.service.IngestionJobService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Optional lightweight scheduler; slow source work is performed by the Python worker. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "fincore.ingestion.scheduling.enabled", havingValue = "true")
public class IngestionScheduling {
  private final IngestionJobService jobs;

  /** Uses the same durable enqueue path as manual refreshes. */
  public IngestionScheduling(IngestionJobService jobs) {
    this.jobs = jobs;
  }

  /** Scans configured due times independently of company/provider refresh intervals. */
  @Scheduled(fixedDelayString = "${fincore.ingestion.scheduling.scan-ms:30000}")
  public void enqueueDue() {
    jobs.scheduleDue();
  }
}

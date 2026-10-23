package com.fincore.server.repository.source;

import com.fincore.server.models.source.IngestionAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for ingestion attempt records. */
public interface IngestionAttemptRepository
    extends JpaRepository<IngestionAttempt, UUID>, JpaSpecificationExecutor<IngestionAttempt> {}

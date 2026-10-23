package com.fincore.server.repository.source;

import com.fincore.server.models.source.NormalizationRun;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for normalization run records. */
public interface NormalizationRunRepository
    extends JpaRepository<NormalizationRun, UUID>, JpaSpecificationExecutor<NormalizationRun> {}

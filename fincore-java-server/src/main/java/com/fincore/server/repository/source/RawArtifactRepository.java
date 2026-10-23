package com.fincore.server.repository.source;

import com.fincore.server.models.source.RawArtifact;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for raw artifact records. */
public interface RawArtifactRepository
    extends JpaRepository<RawArtifact, UUID>, JpaSpecificationExecutor<RawArtifact> {}

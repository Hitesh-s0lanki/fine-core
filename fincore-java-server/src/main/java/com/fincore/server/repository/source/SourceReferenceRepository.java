package com.fincore.server.repository.source;

import com.fincore.server.models.source.SourceReference;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for source reference records. */
public interface SourceReferenceRepository
    extends JpaRepository<SourceReference, UUID>, JpaSpecificationExecutor<SourceReference> {}

package com.fincore.server.repository.source;

import com.fincore.server.models.source.SourceEndpoint;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for source endpoint records. */
public interface SourceEndpointRepository
    extends JpaRepository<SourceEndpoint, UUID>, JpaSpecificationExecutor<SourceEndpoint> {}

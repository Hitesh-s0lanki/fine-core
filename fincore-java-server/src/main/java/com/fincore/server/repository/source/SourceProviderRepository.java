package com.fincore.server.repository.source;

import com.fincore.server.models.source.SourceProvider;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for source provider records. */
public interface SourceProviderRepository
    extends JpaRepository<SourceProvider, UUID>, JpaSpecificationExecutor<SourceProvider> {}

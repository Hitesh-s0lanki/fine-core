package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.ConceptMetricMapping;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for concept metric mapping records. */
public interface ConceptMetricMappingRepository
    extends JpaRepository<ConceptMetricMapping, UUID>,
        JpaSpecificationExecutor<ConceptMetricMapping> {}

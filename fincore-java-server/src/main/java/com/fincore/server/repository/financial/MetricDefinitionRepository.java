package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.MetricDefinition;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for metric definition records. */
public interface MetricDefinitionRepository
    extends JpaRepository<MetricDefinition, UUID>, JpaSpecificationExecutor<MetricDefinition> {}

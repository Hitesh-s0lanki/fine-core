package com.fincore.server.repository.reference;

import com.fincore.server.models.reference.ReferenceDataset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for reference dataset records. */
public interface ReferenceDatasetRepository
    extends JpaRepository<ReferenceDataset, UUID>, JpaSpecificationExecutor<ReferenceDataset> {}

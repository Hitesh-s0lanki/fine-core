package com.fincore.server.repository.reference;

import com.fincore.server.models.reference.ReferenceDatasetObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for reference dataset observation records. */
public interface ReferenceDatasetObservationRepository
    extends JpaRepository<ReferenceDatasetObservation, UUID>,
        JpaSpecificationExecutor<ReferenceDatasetObservation> {}

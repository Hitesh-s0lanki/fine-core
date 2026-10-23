package com.fincore.server.repository.governance;

import com.fincore.server.models.governance.OwnershipObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for ownership observation records. */
public interface OwnershipObservationRepository
    extends JpaRepository<OwnershipObservation, UUID>,
        JpaSpecificationExecutor<OwnershipObservation> {}

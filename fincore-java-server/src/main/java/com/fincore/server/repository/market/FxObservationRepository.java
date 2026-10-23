package com.fincore.server.repository.market;

import com.fincore.server.models.market.FxObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for fx observation records. */
public interface FxObservationRepository
    extends JpaRepository<FxObservation, UUID>, JpaSpecificationExecutor<FxObservation> {}

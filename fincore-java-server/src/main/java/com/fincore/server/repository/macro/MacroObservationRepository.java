package com.fincore.server.repository.macro;

import com.fincore.server.models.macro.MacroObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for macro observation records. */
public interface MacroObservationRepository
    extends JpaRepository<MacroObservation, UUID>, JpaSpecificationExecutor<MacroObservation> {}

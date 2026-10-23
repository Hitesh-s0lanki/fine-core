package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.DerivedMetricObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for derived metric observation records. */
public interface DerivedMetricObservationRepository
    extends JpaRepository<DerivedMetricObservation, UUID>,
        JpaSpecificationExecutor<DerivedMetricObservation> {}

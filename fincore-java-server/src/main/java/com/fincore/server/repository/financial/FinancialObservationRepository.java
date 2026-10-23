package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.FinancialObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for financial observation records. */
public interface FinancialObservationRepository
    extends JpaRepository<FinancialObservation, UUID>,
        JpaSpecificationExecutor<FinancialObservation> {}

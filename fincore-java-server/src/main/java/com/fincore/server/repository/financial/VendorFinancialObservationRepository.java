package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.VendorFinancialObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for vendor financial observation records. */
public interface VendorFinancialObservationRepository
    extends JpaRepository<VendorFinancialObservation, UUID>,
        JpaSpecificationExecutor<VendorFinancialObservation> {}

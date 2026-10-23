package com.fincore.server.repository.credit;

import com.fincore.server.models.credit.CreditRatingObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for credit rating observation records. */
public interface CreditRatingObservationRepository
    extends JpaRepository<CreditRatingObservation, UUID>,
        JpaSpecificationExecutor<CreditRatingObservation> {}

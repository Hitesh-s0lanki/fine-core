package com.fincore.server.repository.market;

import com.fincore.server.models.market.MarketMetricObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for market metric observation records. */
public interface MarketMetricObservationRepository
    extends JpaRepository<MarketMetricObservation, UUID>,
        JpaSpecificationExecutor<MarketMetricObservation> {}

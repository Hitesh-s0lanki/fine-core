package com.fincore.server.repository.market;

import com.fincore.server.models.market.PriceBar;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for price bar records. */
public interface PriceBarRepository
    extends JpaRepository<PriceBar, UUID>, JpaSpecificationExecutor<PriceBar> {}

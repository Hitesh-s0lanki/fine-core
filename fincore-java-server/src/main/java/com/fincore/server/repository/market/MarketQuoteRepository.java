package com.fincore.server.repository.market;

import com.fincore.server.models.market.MarketQuote;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for market quote records. */
public interface MarketQuoteRepository
    extends JpaRepository<MarketQuote, UUID>, JpaSpecificationExecutor<MarketQuote> {}

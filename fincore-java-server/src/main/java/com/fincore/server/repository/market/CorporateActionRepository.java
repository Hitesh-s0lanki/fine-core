package com.fincore.server.repository.market;

import com.fincore.server.models.market.CorporateAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for corporate action records. */
public interface CorporateActionRepository
    extends JpaRepository<CorporateAction, UUID>, JpaSpecificationExecutor<CorporateAction> {}

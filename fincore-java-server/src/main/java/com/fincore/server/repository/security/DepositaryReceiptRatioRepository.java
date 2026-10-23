package com.fincore.server.repository.security;

import com.fincore.server.models.security.DepositaryReceiptRatio;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for depositary receipt ratio records. */
public interface DepositaryReceiptRatioRepository
    extends JpaRepository<DepositaryReceiptRatio, UUID>,
        JpaSpecificationExecutor<DepositaryReceiptRatio> {}

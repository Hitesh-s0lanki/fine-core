package com.fincore.server.repository.credit;

import com.fincore.server.models.credit.DebtInstrument;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for debt instrument records. */
public interface DebtInstrumentRepository
    extends JpaRepository<DebtInstrument, UUID>, JpaSpecificationExecutor<DebtInstrument> {}

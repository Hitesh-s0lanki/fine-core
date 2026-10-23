package com.fincore.server.repository.disclosure;

import com.fincore.server.models.disclosure.FinancialResultDisclosure;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for financial result disclosure records. */
public interface FinancialResultDisclosureRepository
    extends JpaRepository<FinancialResultDisclosure, UUID>,
        JpaSpecificationExecutor<FinancialResultDisclosure> {}

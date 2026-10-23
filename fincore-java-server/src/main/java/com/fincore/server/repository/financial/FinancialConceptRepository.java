package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.FinancialConcept;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for financial concept records. */
public interface FinancialConceptRepository
    extends JpaRepository<FinancialConcept, UUID>, JpaSpecificationExecutor<FinancialConcept> {}

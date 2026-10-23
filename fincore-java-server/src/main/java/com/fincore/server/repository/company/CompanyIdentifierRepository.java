package com.fincore.server.repository.company;

import com.fincore.server.models.company.CompanyIdentifier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for company identifier records. */
public interface CompanyIdentifierRepository
    extends JpaRepository<CompanyIdentifier, UUID>, JpaSpecificationExecutor<CompanyIdentifier> {}

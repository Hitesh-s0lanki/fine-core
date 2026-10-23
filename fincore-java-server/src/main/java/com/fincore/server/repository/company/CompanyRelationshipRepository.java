package com.fincore.server.repository.company;

import com.fincore.server.models.company.CompanyRelationship;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for company relationship records. */
public interface CompanyRelationshipRepository
    extends JpaRepository<CompanyRelationship, UUID>,
        JpaSpecificationExecutor<CompanyRelationship> {}

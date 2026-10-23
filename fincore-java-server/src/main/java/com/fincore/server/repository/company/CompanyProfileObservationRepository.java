package com.fincore.server.repository.company;

import com.fincore.server.models.company.CompanyProfileObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for company profile observation records. */
public interface CompanyProfileObservationRepository
    extends JpaRepository<CompanyProfileObservation, UUID>,
        JpaSpecificationExecutor<CompanyProfileObservation> {}

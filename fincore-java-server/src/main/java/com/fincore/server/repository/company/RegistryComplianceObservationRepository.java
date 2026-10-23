package com.fincore.server.repository.company;

import com.fincore.server.models.company.RegistryComplianceObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for registry compliance observation records. */
public interface RegistryComplianceObservationRepository
    extends JpaRepository<RegistryComplianceObservation, UUID>,
        JpaSpecificationExecutor<RegistryComplianceObservation> {}

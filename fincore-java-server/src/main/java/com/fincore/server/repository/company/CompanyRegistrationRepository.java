package com.fincore.server.repository.company;

import com.fincore.server.models.company.CompanyRegistration;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for company registration records. */
public interface CompanyRegistrationRepository
    extends JpaRepository<CompanyRegistration, UUID>,
        JpaSpecificationExecutor<CompanyRegistration> {}

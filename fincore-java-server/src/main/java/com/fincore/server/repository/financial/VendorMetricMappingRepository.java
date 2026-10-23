package com.fincore.server.repository.financial;

import com.fincore.server.models.financial.VendorMetricMapping;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for vendor metric mapping records. */
public interface VendorMetricMappingRepository
    extends JpaRepository<VendorMetricMapping, UUID>,
        JpaSpecificationExecutor<VendorMetricMapping> {}

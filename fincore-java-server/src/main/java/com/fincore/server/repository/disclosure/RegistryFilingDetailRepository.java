package com.fincore.server.repository.disclosure;

import com.fincore.server.models.disclosure.RegistryFilingDetail;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for registry filing detail records. */
public interface RegistryFilingDetailRepository
    extends JpaRepository<RegistryFilingDetail, UUID>,
        JpaSpecificationExecutor<RegistryFilingDetail> {}

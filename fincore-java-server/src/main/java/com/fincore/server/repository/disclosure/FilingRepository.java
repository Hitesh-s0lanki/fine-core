package com.fincore.server.repository.disclosure;

import com.fincore.server.models.disclosure.Filing;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for filing records. */
public interface FilingRepository
    extends JpaRepository<Filing, UUID>, JpaSpecificationExecutor<Filing> {}

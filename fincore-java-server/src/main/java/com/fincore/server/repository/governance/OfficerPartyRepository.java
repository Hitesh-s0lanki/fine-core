package com.fincore.server.repository.governance;

import com.fincore.server.models.governance.OfficerParty;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for officer party records. */
public interface OfficerPartyRepository
    extends JpaRepository<OfficerParty, UUID>, JpaSpecificationExecutor<OfficerParty> {}

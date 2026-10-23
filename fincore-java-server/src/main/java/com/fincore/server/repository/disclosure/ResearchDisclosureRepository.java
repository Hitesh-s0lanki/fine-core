package com.fincore.server.repository.disclosure;

import com.fincore.server.models.disclosure.ResearchDisclosure;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for research disclosure records. */
public interface ResearchDisclosureRepository
    extends JpaRepository<ResearchDisclosure, UUID>, JpaSpecificationExecutor<ResearchDisclosure> {}

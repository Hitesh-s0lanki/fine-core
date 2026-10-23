package com.fincore.server.repository.disclosure;

import com.fincore.server.models.disclosure.DocumentLink;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for document link records. */
public interface DocumentLinkRepository
    extends JpaRepository<DocumentLink, UUID>, JpaSpecificationExecutor<DocumentLink> {}

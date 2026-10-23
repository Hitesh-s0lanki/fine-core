package com.fincore.server.repository.security;

import com.fincore.server.models.security.SecurityIdentifier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for security identifier records. */
public interface SecurityIdentifierRepository
    extends JpaRepository<SecurityIdentifier, UUID>, JpaSpecificationExecutor<SecurityIdentifier> {}

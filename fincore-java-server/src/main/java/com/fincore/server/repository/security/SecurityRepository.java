package com.fincore.server.repository.security;

import com.fincore.server.models.security.Security;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for security records. */
public interface SecurityRepository
    extends JpaRepository<Security, UUID>, JpaSpecificationExecutor<Security> {}

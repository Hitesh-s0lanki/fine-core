package com.fincore.server.repository.security;

import com.fincore.server.models.security.Listing;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for listing records. */
public interface ListingRepository
    extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {}

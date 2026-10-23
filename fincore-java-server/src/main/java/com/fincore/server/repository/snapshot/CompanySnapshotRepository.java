package com.fincore.server.repository.snapshot;

import com.fincore.server.models.snapshot.CompanySnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for company snapshot records. */
public interface CompanySnapshotRepository
    extends JpaRepository<CompanySnapshot, UUID>, JpaSpecificationExecutor<CompanySnapshot> {}

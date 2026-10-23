package com.fincore.server.repository.quality;

import com.fincore.server.models.quality.CoverageItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for coverage item records. */
public interface CoverageItemRepository
    extends JpaRepository<CoverageItem, UUID>, JpaSpecificationExecutor<CoverageItem> {}

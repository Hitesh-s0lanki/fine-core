package com.fincore.server.repository.quality;

import com.fincore.server.models.quality.DataQualityIssue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for data quality issue records. */
public interface DataQualityIssueRepository
    extends JpaRepository<DataQualityIssue, UUID>, JpaSpecificationExecutor<DataQualityIssue> {}

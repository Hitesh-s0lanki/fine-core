package com.fincore.server.repository.macro;

import com.fincore.server.models.macro.MacroSeries;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for macro series records. */
public interface MacroSeriesRepository
    extends JpaRepository<MacroSeries, UUID>, JpaSpecificationExecutor<MacroSeries> {}

package com.fincore.server.repository.company;

import com.fincore.server.models.company.CompanyIdentifier;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence and bounded query access for company identifier records. */
public interface CompanyIdentifierRepository
    extends JpaRepository<CompanyIdentifier, UUID>, JpaSpecificationExecutor<CompanyIdentifier> {
  /** Finds canonical CIK matches without depending on provider authority capitalization. */
  @Query("select i from CompanyIdentifier i where i.scheme = 'cik' and i.value in :values")
  List<CompanyIdentifier> findCiks(@Param("values") Collection<String> values);
}

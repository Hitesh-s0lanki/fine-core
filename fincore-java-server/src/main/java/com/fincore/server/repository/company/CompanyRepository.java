package com.fincore.server.repository.company;

import com.fincore.server.models.company.Company;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Typed company persistence and issuer search through names, identifiers and listings. */
public interface CompanyRepository
    extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {
  /** Searches without treating user percent/underscore characters as SQL wildcards. */
  @Query(
      """
      select c from Company c where :query = ''
      or locate(lower(:query), lower(c.displayName)) > 0
      or lower(c.slug) = lower(:query)
      or exists (select i.id from CompanyIdentifier i
         where i.companyId = c.id and lower(i.value) = lower(:query))
      or exists (select l.id from Listing l join l.securityReference s
         where s.companyId = c.id and lower(l.symbol) = lower(:query))
      """)
  Page<Company> search(@Param("query") String query, Pageable pageable);
}

package com.fincore.server.service;

import com.fincore.server.dto.common.RecordPage;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.company.CompanyIdentifierRequest;
import com.fincore.server.dto.company.CompanyOnboardingRequest;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.CompanyServiceInter;
import com.fincore.server.models.company.Company;
import com.fincore.server.repository.company.CompanyRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Company-specific application service with atomic issuer identity creation. */
@Service
@Transactional(readOnly = true)
public class CompanyService implements CompanyServiceInter {
  private final CompanyRepository companies;
  private final CompanyDataServiceInter data;

  /** Uses typed company persistence and shared provenance validation. */
  public CompanyService(CompanyRepository companies, CompanyDataServiceInter data) {
    this.companies = companies;
    this.data = data;
  }

  /** Saves the company and its initial source-scoped identifiers atomically. */
  @Transactional
  public RecordView create(CompanyOnboardingRequest request) {
    Company company = (Company) data.save(data.resource("companies"), request.company());
    for (var identifier : request.identifiers()) {
      data.save(
          data.resource("company-identifiers"),
          new CompanyIdentifierRequest(
              identifier.evidenceIds(),
              company.getId(),
              identifier.scheme(),
              identifier.authority(),
              identifier.value(),
              identifier.validFrom(),
              identifier.validTo()));
    }
    return data.view(company);
  }

  /** Searches issuer names, slugs, identifiers and listing symbols with bounded pagination. */
  public RecordPage search(String query, int page, int size) {
    if (page < 0 || size < 1 || size > 100 || query.length() > 100) {
      throw new IllegalArgumentException("Invalid company search or pagination");
    }
    var result =
        companies.search(query.trim(), PageRequest.of(page, size, Sort.by("displayName", "id")));
    return new RecordPage(
        result.map(data::view).getContent(),
        page,
        size,
        result.getTotalElements(),
        result.hasNext());
  }

  /** Reads a company by its canonical UUID. */
  public RecordView get(UUID id) {
    return data.get("companies", id);
  }
}

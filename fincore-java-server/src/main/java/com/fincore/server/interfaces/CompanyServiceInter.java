package com.fincore.server.interfaces;

import com.fincore.server.dto.common.RecordPage;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.company.CompanyOnboardingRequest;
import java.util.UUID;

/** Application contract for company onboarding, search and identity reads. */
public interface CompanyServiceInter {
  /** Registers an evidenced company and initial identifiers atomically. */
  RecordView create(CompanyOnboardingRequest request);

  /** Searches company names, source identifiers and listing symbols. */
  RecordPage search(String query, int page, int size);

  /** Reads an issuer by its canonical identifier. */
  RecordView get(UUID id);
}

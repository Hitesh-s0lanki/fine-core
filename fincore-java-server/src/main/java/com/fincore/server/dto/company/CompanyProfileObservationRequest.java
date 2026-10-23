package com.fincore.server.dto.company;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.CompanyProfileObservation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for company profile observation. */
public record CompanyProfileObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull
        @Pattern(
            regexp =
                "legal_name|trading_name|former_name|description|"
                    + "legal_form|phone|domicile_country|incorporation_country|"
                    + "website|investor_relations_url|address|"
                    + "industry_classification|fiscal_year_end|"
                    + "reporting_currency|sector_classification|employee_count|"
                    + "logo_url|executive_summary")
        String field,
    @NotNull Object profileValue,
    LocalDate effectiveFrom,
    LocalDate effectiveTo)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    CompanyProfileObservation entity = new CompanyProfileObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setField(field);
    entity.setProfileValue(profileValue);
    entity.setEffectiveFrom(effectiveFrom);
    entity.setEffectiveTo(effectiveTo);
    return entity;
  }
}

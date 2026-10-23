package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.DocumentLink;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for document link. */
public record DocumentLinkRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID documentId,
    UUID companyId,
    UUID filingId,
    UUID announcementId,
    @NotNull
        @Pattern(
            regexp = "primary|attachment|xbrl|annual_report|presentation|investor_relations|other")
        String role)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    DocumentLink entity = new DocumentLink();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setDocumentId(documentId);
    entity.setCompanyId(companyId);
    entity.setFilingId(filingId);
    entity.setAnnouncementId(announcementId);
    entity.setRole(role);
    return entity;
  }
}

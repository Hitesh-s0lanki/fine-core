package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.ResearchDisclosure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for research disclosure. */
public record ResearchDisclosureRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    UUID filingId,
    @NotNull UUID documentId,
    @NotNull @Pattern(regexp = "risk|litigation|strategy|other") String topic,
    @NotBlank @Size(max = 40000) String extractedText,
    @NotBlank @Size(max = 40000) String pageSection,
    @NotBlank @Size(max = 40000) String extractionVersion)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    ResearchDisclosure entity = new ResearchDisclosure();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setFilingId(filingId);
    entity.setDocumentId(documentId);
    entity.setTopic(topic);
    entity.setExtractedText(extractedText);
    entity.setPageSection(pageSection);
    entity.setExtractionVersion(extractionVersion);
    return entity;
  }
}

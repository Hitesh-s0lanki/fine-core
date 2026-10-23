package com.fincore.server.dto.disclosure;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.disclosure.Announcement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for announcement. */
public record AnnouncementRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    UUID listingId,
    @NotNull UUID providerId,
    @NotBlank @Size(max = 40000) String sourceRecordId,
    @Size(max = 40000) String categoryRaw,
    @NotBlank @Size(max = 40000) String title,
    @Size(max = 40000) String body,
    Instant publishedAt,
    @NotBlank @Size(max = 40000) String publishedAtRaw,
    @Size(max = 40000) String timestampTimezone,
    Instant disseminatedAt)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    Announcement entity = new Announcement();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setListingId(listingId);
    entity.setProviderId(providerId);
    entity.setSourceRecordId(sourceRecordId);
    entity.setCategoryRaw(categoryRaw);
    entity.setTitle(title);
    entity.setBody(body);
    entity.setPublishedAt(publishedAt);
    entity.setPublishedAtRaw(publishedAtRaw);
    entity.setTimestampTimezone(timestampTimezone);
    entity.setDisseminatedAt(disseminatedAt);
    return entity;
  }
}

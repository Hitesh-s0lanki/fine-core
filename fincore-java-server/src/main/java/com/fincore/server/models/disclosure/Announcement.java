package com.fincore.server.models.disclosure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.security.Listing;
import com.fincore.server.models.source.SourceProvider;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent announcement assertion. */
@Entity
@Table(name = "announcement")
@DiscriminatorValue("announcements")
@Getter
@Setter
@NoArgsConstructor
public class Announcement extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "listing_id", nullable = true)
  private UUID listingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "listing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Listing listingReference;

  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private SourceProvider providerReference;

  @Column(name = "source_record_id", nullable = false)
  private String sourceRecordId;

  @Column(name = "category_raw", nullable = true)
  private String categoryRaw;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "body", nullable = true)
  private String body;

  @Column(name = "published_at", nullable = true)
  private Instant publishedAt;

  @Column(name = "published_at_raw", nullable = false)
  private String publishedAtRaw;

  @Column(name = "timestamp_timezone", nullable = true)
  private String timestampTimezone;

  @Column(name = "disseminated_at", nullable = true)
  private Instant disseminatedAt;
}

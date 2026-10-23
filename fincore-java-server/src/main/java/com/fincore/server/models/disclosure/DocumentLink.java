package com.fincore.server.models.disclosure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent document link assertion. */
@Entity
@Table(name = "document_link")
@DiscriminatorValue("document-links")
@Getter
@Setter
@NoArgsConstructor
public class DocumentLink extends DomainRecord {
  @Column(name = "document_id", nullable = false)
  private UUID documentId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Document documentReference;

  @Column(name = "company_id", nullable = true)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "filing_id", nullable = true)
  private UUID filingId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "filing_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Filing filingReference;

  @Column(name = "announcement_id", nullable = true)
  private UUID announcementId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "announcement_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Announcement announcementReference;

  @Column(name = "role", nullable = false)
  private String role;
}

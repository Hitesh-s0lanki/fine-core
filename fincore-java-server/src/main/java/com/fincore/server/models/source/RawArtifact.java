package com.fincore.server.models.source;

import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent raw artifact assertion. */
@Entity
@Table(name = "raw_artifact")
@DiscriminatorValue("raw-artifacts")
@Getter
@Setter
@NoArgsConstructor
public class RawArtifact extends DomainRecord {
  @Column(name = "storage_uri", nullable = false)
  private String storageUri;

  @Column(name = "sha256", nullable = false)
  private String sha256;

  @Column(name = "hash_basis", nullable = false)
  private String hashBasis;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "byte_length", nullable = false)
  private Long byteLength;

  @Column(name = "format", nullable = false)
  private String format;

  @Column(name = "stored_at", nullable = false)
  private Instant storedAt;

  @Column(name = "original_response_sha256", nullable = true)
  private String originalResponseSha256;
}

package com.fincore.server.interfaces;

import com.fincore.server.models.common.DomainRecord;

/** Maps a validated creation DTO to a new canonical assertion. */
public interface CreationRequest {
  /** Creates a record without accepting server-owned identity fields. */
  DomainRecord toEntity();
}

package com.fincore.server.support;

import com.fincore.server.models.common.DomainRecord;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

/** Supplies persistence-owned identity metadata for detached unit-test fixtures. */
public final class TestRecords {
  private TestRecords() {}

  /** Assigns a known ID without changing the production entity's public API. */
  public static <T extends DomainRecord> T identified(T record, UUID id) {
    ReflectionTestUtils.setField(record, "id", id);
    ReflectionTestUtils.setField(record, "createdAt", Instant.parse("2026-10-22T12:00:00Z"));
    return record;
  }
}

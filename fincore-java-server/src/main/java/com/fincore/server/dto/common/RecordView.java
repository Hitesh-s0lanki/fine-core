package com.fincore.server.dto.common;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One canonical assertion with server audit metadata and source evidence. */
public record RecordView(
    UUID id,
    String recordType,
    int schemaVersion,
    Instant createdAt,
    Set<UUID> evidenceIds,
    Map<String, Object> attributes) {}

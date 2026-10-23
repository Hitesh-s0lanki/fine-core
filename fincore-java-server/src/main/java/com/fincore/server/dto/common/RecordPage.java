package com.fincore.server.dto.common;

import java.util.List;

/** Bounded records with explicit pagination metadata. */
public record RecordPage(
    List<RecordView> items, int page, int size, long totalItems, boolean hasNext) {}

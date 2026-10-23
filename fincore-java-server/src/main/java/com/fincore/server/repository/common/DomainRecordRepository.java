package com.fincore.server.repository.common;

import com.fincore.server.models.common.DomainRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Resolves polymorphic references without losing their concrete record type. */
public interface DomainRecordRepository extends JpaRepository<DomainRecord, UUID> {
  /** Resolves a record's type without joining every canonical subtype table. */
  @Query(value = "select record_type from data_record where id = :id", nativeQuery = true)
  Optional<String> findResourceById(@Param("id") UUID id);
}

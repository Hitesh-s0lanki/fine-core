package com.fincore.server.interfaces;

import com.fincore.server.config.Resources;
import com.fincore.server.dto.common.RecordPage;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.models.common.DomainRecord;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Application contract for canonical assertion creation, validation and bounded reads. */
public interface CompanyDataServiceInter {
  /** Creates an assertion using the resource's strict creation DTO. */
  RecordView create(String route, JsonNode input);

  /** Persists an internal typed creation request within a service transaction. */
  DomainRecord save(Resources.Resource resource, CreationRequest request);

  /** Reads an assertion after verifying its resource type. */
  RecordView get(String route, UUID id);

  /** Loads a typed entity for application-service operations. */
  DomainRecord load(String route, UUID id);

  /** Queries bounded records using allowlisted filters. */
  RecordPage list(String route, int page, int size, Map<String, String> filters);

  /** Detaches an assertion into its stable API representation. */
  RecordView view(DomainRecord record);

  /** Resolves a supported resource definition. */
  Resources.Resource resource(String route);
}

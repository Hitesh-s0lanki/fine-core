package com.fincore.server.controller;

import com.fincore.server.config.Resources;
import com.fincore.server.dto.common.RecordPage;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/** REST access to explicitly typed canonical resources, not arbitrary entity classes. */
@RestController
@RequestMapping("/api/v1/company-data")
public class CompanyDataController {
  private final CompanyDataServiceInter service;

  /** Uses the transactional company data service. */
  public CompanyDataController(CompanyDataServiceInter service) {
    this.service = service;
  }

  /** Lists supported resource names and their creation request contracts. */
  @GetMapping
  public List<Map<String, String>> resources() {
    return Resources.ALL.stream()
        .map(r -> Map.of("resource", r.route(), "requestType", r.requestType().getSimpleName()))
        .toList();
  }

  /** Creates an assertion; all server-owned identity fields are excluded from its DTO. */
  @PostMapping("/{resource}")
  public ResponseEntity<RecordView> create(
      @PathVariable String resource, @RequestBody JsonNode request) {
    RecordView result = service.create(resource, request);
    return ResponseEntity.created(
            URI.create("/api/v1/company-data/" + resource + "/" + result.id()))
        .body(result);
  }

  /** Reads one resource by its immutable UUID. */
  @GetMapping("/{resource}/{id}")
  public RecordView get(@PathVariable String resource, @PathVariable UUID id) {
    return service.get(resource, id);
  }

  /** Lists assertions with explicit pagination and optional entity/period filters. */
  @GetMapping("/{resource}")
  public RecordPage list(
      @PathVariable String resource,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      @RequestParam Map<String, String> parameters) {
    Map<String, String> filters = new HashMap<>(parameters);
    filters.keySet().removeAll(Set.of("page", "size"));
    return service.list(resource, page, size, filters);
  }
}

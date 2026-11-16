package com.fincore.server.controller;

import com.fincore.server.dto.discovery.DiscoveryContracts.ImportRequest;
import com.fincore.server.dto.discovery.DiscoveryContracts.Imported;
import com.fincore.server.dto.discovery.DiscoveryContracts.Results;
import com.fincore.server.service.CompanyDiscoveryService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Frontend entry points for SEC directory search and explicit company onboarding. */
@RestController
@RequestMapping("/api/v1/companies/discovery/sec")
public class CompanyDiscoveryController {
  private final CompanyDiscoveryService service;

  /** Uses the identity discovery application service. */
  public CompanyDiscoveryController(CompanyDiscoveryService service) {
    this.service = service;
  }

  /** Returns bounded directory matches including an existing local company ID when present. */
  @GetMapping
  public Results search(
      @RequestParam String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return service.search(query, page, size);
  }

  /** Returns 201 for a new identity or 200 when the CIK is already stored. */
  @PostMapping("/import")
  public ResponseEntity<Imported> importCompany(@Valid @RequestBody ImportRequest request) {
    Imported result = service.importCompany(request.cik());
    return result.created()
        ? ResponseEntity.created(URI.create("/api/v1/companies/" + result.company().id()))
            .body(result)
        : ResponseEntity.ok(result);
  }
}

package com.fincore.server.controller;

import com.fincore.server.dto.common.RecordPage;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.dto.company.CompanyOnboardingRequest;
import com.fincore.server.dto.company.CompanyOverview;
import com.fincore.server.dto.snapshot.SnapshotRequest;
import com.fincore.server.interfaces.CompanyServiceInter;
import com.fincore.server.interfaces.SnapshotServiceInter;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Company search, atomic onboarding and immutable snapshot overview endpoints. */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
  private final CompanyServiceInter companies;
  private final SnapshotServiceInter snapshots;

  /** Uses company and snapshot application services. */
  public CompanyController(CompanyServiceInter companies, SnapshotServiceInter snapshots) {
    this.companies = companies;
    this.snapshots = snapshots;
  }

  /** Creates an evidenced company and its required initial identifiers. */
  @PostMapping
  public ResponseEntity<RecordView> create(@Valid @RequestBody CompanyOnboardingRequest request) {
    RecordView result = companies.create(request);
    return ResponseEntity.created(URI.create("/api/v1/companies/" + result.id())).body(result);
  }

  /** Searches company names, identifiers and ticker symbols. */
  @GetMapping
  public RecordPage search(
      @RequestParam(defaultValue = "") String query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return companies.search(query, page, size);
  }

  /** Reads the company master record. */
  @GetMapping("/{companyId}")
  public RecordView get(@PathVariable UUID companyId) {
    return companies.get(companyId);
  }

  /** Commits a reviewed selection manifest and its financial cells atomically. */
  @PostMapping("/{companyId}/snapshots")
  public ResponseEntity<RecordView> snapshot(
      @PathVariable UUID companyId, @Valid @RequestBody SnapshotRequest request) {
    RecordView result = snapshots.create(companyId, request);
    return ResponseEntity.created(URI.create("/api/v1/company-data/snapshots/" + result.id()))
        .body(result);
  }

  /** Reads an overview from a specified immutable snapshot. */
  @GetMapping("/{companyId}/overview")
  public CompanyOverview overview(@PathVariable UUID companyId, @RequestParam UUID snapshotId) {
    return snapshots.overview(companyId, snapshotId);
  }
}

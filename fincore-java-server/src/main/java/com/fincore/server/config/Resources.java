package com.fincore.server.config;

import com.fincore.server.dto.company.CompanyIdentifierRequest;
import com.fincore.server.dto.company.CompanyProfileObservationRequest;
import com.fincore.server.dto.company.CompanyRegistrationRequest;
import com.fincore.server.dto.company.CompanyRelationshipRequest;
import com.fincore.server.dto.company.CompanyRequest;
import com.fincore.server.dto.company.RegistryComplianceObservationRequest;
import com.fincore.server.dto.credit.CreditRatingObservationRequest;
import com.fincore.server.dto.credit.DebtInstrumentRequest;
import com.fincore.server.dto.disclosure.AnnouncementRequest;
import com.fincore.server.dto.disclosure.DocumentLinkRequest;
import com.fincore.server.dto.disclosure.DocumentRequest;
import com.fincore.server.dto.disclosure.FilingRequest;
import com.fincore.server.dto.disclosure.FinancialResultDisclosureRequest;
import com.fincore.server.dto.disclosure.RegistryFilingDetailRequest;
import com.fincore.server.dto.disclosure.ResearchDisclosureRequest;
import com.fincore.server.dto.financial.ConceptMetricMappingRequest;
import com.fincore.server.dto.financial.DerivedMetricObservationRequest;
import com.fincore.server.dto.financial.FinancialConceptRequest;
import com.fincore.server.dto.financial.FinancialObservationRequest;
import com.fincore.server.dto.financial.MetricDefinitionRequest;
import com.fincore.server.dto.financial.VendorFinancialObservationRequest;
import com.fincore.server.dto.financial.VendorMetricMappingRequest;
import com.fincore.server.dto.governance.OfficerAppointmentRequest;
import com.fincore.server.dto.governance.OfficerPartyRequest;
import com.fincore.server.dto.governance.OwnershipObservationRequest;
import com.fincore.server.dto.macro.MacroObservationRequest;
import com.fincore.server.dto.macro.MacroSeriesRequest;
import com.fincore.server.dto.market.CorporateActionRequest;
import com.fincore.server.dto.market.FxObservationRequest;
import com.fincore.server.dto.market.MarketMetricObservationRequest;
import com.fincore.server.dto.market.MarketQuoteRequest;
import com.fincore.server.dto.market.PriceBarRequest;
import com.fincore.server.dto.quality.CoverageItemRequest;
import com.fincore.server.dto.quality.DataQualityIssueRequest;
import com.fincore.server.dto.reference.ReferenceDatasetObservationRequest;
import com.fincore.server.dto.reference.ReferenceDatasetRequest;
import com.fincore.server.dto.security.DepositaryReceiptRatioRequest;
import com.fincore.server.dto.security.ListingRequest;
import com.fincore.server.dto.security.SecurityIdentifierRequest;
import com.fincore.server.dto.security.SecurityRequest;
import com.fincore.server.dto.snapshot.CompanySnapshotRequest;
import com.fincore.server.dto.snapshot.SnapshotFinancialCellRequest;
import com.fincore.server.dto.snapshot.SnapshotMemberRequest;
import com.fincore.server.dto.source.IngestionAttemptRequest;
import com.fincore.server.dto.source.NormalizationRunRequest;
import com.fincore.server.dto.source.ProviderEntityBindingRequest;
import com.fincore.server.dto.source.RawArtifactRequest;
import com.fincore.server.dto.source.SourceEndpointRequest;
import com.fincore.server.dto.source.SourceProviderRequest;
import com.fincore.server.dto.source.SourceReferenceRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.company.CompanyIdentifier;
import com.fincore.server.models.company.CompanyProfileObservation;
import com.fincore.server.models.company.CompanyRegistration;
import com.fincore.server.models.company.CompanyRelationship;
import com.fincore.server.models.company.RegistryComplianceObservation;
import com.fincore.server.models.credit.CreditRatingObservation;
import com.fincore.server.models.credit.DebtInstrument;
import com.fincore.server.models.disclosure.Announcement;
import com.fincore.server.models.disclosure.Document;
import com.fincore.server.models.disclosure.DocumentLink;
import com.fincore.server.models.disclosure.Filing;
import com.fincore.server.models.disclosure.FinancialResultDisclosure;
import com.fincore.server.models.disclosure.RegistryFilingDetail;
import com.fincore.server.models.disclosure.ResearchDisclosure;
import com.fincore.server.models.financial.ConceptMetricMapping;
import com.fincore.server.models.financial.DerivedMetricObservation;
import com.fincore.server.models.financial.FinancialConcept;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.models.financial.MetricDefinition;
import com.fincore.server.models.financial.VendorFinancialObservation;
import com.fincore.server.models.financial.VendorMetricMapping;
import com.fincore.server.models.governance.OfficerAppointment;
import com.fincore.server.models.governance.OfficerParty;
import com.fincore.server.models.governance.OwnershipObservation;
import com.fincore.server.models.macro.MacroObservation;
import com.fincore.server.models.macro.MacroSeries;
import com.fincore.server.models.market.CorporateAction;
import com.fincore.server.models.market.FxObservation;
import com.fincore.server.models.market.MarketMetricObservation;
import com.fincore.server.models.market.MarketQuote;
import com.fincore.server.models.market.PriceBar;
import com.fincore.server.models.quality.CoverageItem;
import com.fincore.server.models.quality.DataQualityIssue;
import com.fincore.server.models.reference.ReferenceDataset;
import com.fincore.server.models.reference.ReferenceDatasetObservation;
import com.fincore.server.models.security.DepositaryReceiptRatio;
import com.fincore.server.models.security.Listing;
import com.fincore.server.models.security.Security;
import com.fincore.server.models.security.SecurityIdentifier;
import com.fincore.server.models.snapshot.CompanySnapshot;
import com.fincore.server.models.snapshot.SnapshotFinancialCell;
import com.fincore.server.models.snapshot.SnapshotMember;
import com.fincore.server.models.source.IngestionAttempt;
import com.fincore.server.models.source.NormalizationRun;
import com.fincore.server.models.source.ProviderEntityBinding;
import com.fincore.server.models.source.RawArtifact;
import com.fincore.server.models.source.SourceEndpoint;
import com.fincore.server.models.source.SourceProvider;
import com.fincore.server.models.source.SourceReference;
import java.util.List;

/** Allowlisted canonical resources; never resolves arbitrary class names from HTTP. */
public final class Resources {
  private Resources() {}

  /** Couples each route to its entity and validated input DTO. */
  public record Resource(
      String route,
      Class<? extends DomainRecord> entityType,
      Class<? extends com.fincore.server.interfaces.CreationRequest> requestType) {}

  /** Canonical schema resources supported by the server. */
  public static final List<Resource> ALL =
      List.of(
          new Resource("companies", Company.class, CompanyRequest.class),
          new Resource("providers", SourceProvider.class, SourceProviderRequest.class),
          new Resource("source-endpoints", SourceEndpoint.class, SourceEndpointRequest.class),
          new Resource(
              "provider-bindings", ProviderEntityBinding.class, ProviderEntityBindingRequest.class),
          new Resource("raw-artifacts", RawArtifact.class, RawArtifactRequest.class),
          new Resource("ingestion-attempts", IngestionAttempt.class, IngestionAttemptRequest.class),
          new Resource("normalization-runs", NormalizationRun.class, NormalizationRunRequest.class),
          new Resource("source-references", SourceReference.class, SourceReferenceRequest.class),
          new Resource(
              "company-identifiers", CompanyIdentifier.class, CompanyIdentifierRequest.class),
          new Resource(
              "profile-observations",
              CompanyProfileObservation.class,
              CompanyProfileObservationRequest.class),
          new Resource(
              "registrations", CompanyRegistration.class, CompanyRegistrationRequest.class),
          new Resource(
              "company-relationships", CompanyRelationship.class, CompanyRelationshipRequest.class),
          new Resource("securities", Security.class, SecurityRequest.class),
          new Resource(
              "security-identifiers", SecurityIdentifier.class, SecurityIdentifierRequest.class),
          new Resource("listings", Listing.class, ListingRequest.class),
          new Resource(
              "depositary-receipt-ratios",
              DepositaryReceiptRatio.class,
              DepositaryReceiptRatioRequest.class),
          new Resource("filings", Filing.class, FilingRequest.class),
          new Resource(
              "financial-results",
              FinancialResultDisclosure.class,
              FinancialResultDisclosureRequest.class),
          new Resource("announcements", Announcement.class, AnnouncementRequest.class),
          new Resource("documents", Document.class, DocumentRequest.class),
          new Resource("document-links", DocumentLink.class, DocumentLinkRequest.class),
          new Resource(
              "registry-filing-details",
              RegistryFilingDetail.class,
              RegistryFilingDetailRequest.class),
          new Resource(
              "registry-compliance",
              RegistryComplianceObservation.class,
              RegistryComplianceObservationRequest.class),
          new Resource("financial-concepts", FinancialConcept.class, FinancialConceptRequest.class),
          new Resource("metric-definitions", MetricDefinition.class, MetricDefinitionRequest.class),
          new Resource(
              "concept-metric-mappings",
              ConceptMetricMapping.class,
              ConceptMetricMappingRequest.class),
          new Resource(
              "vendor-metric-mappings",
              VendorMetricMapping.class,
              VendorMetricMappingRequest.class),
          new Resource(
              "financial-observations",
              FinancialObservation.class,
              FinancialObservationRequest.class),
          new Resource(
              "vendor-financial-observations",
              VendorFinancialObservation.class,
              VendorFinancialObservationRequest.class),
          new Resource(
              "derived-metric-observations",
              DerivedMetricObservation.class,
              DerivedMetricObservationRequest.class),
          new Resource("market-quotes", MarketQuote.class, MarketQuoteRequest.class),
          new Resource("price-bars", PriceBar.class, PriceBarRequest.class),
          new Resource(
              "market-metric-observations",
              MarketMetricObservation.class,
              MarketMetricObservationRequest.class),
          new Resource("corporate-actions", CorporateAction.class, CorporateActionRequest.class),
          new Resource("fx-observations", FxObservation.class, FxObservationRequest.class),
          new Resource("macro-series", MacroSeries.class, MacroSeriesRequest.class),
          new Resource("macro-observations", MacroObservation.class, MacroObservationRequest.class),
          new Resource("coverage-items", CoverageItem.class, CoverageItemRequest.class),
          new Resource("quality-issues", DataQualityIssue.class, DataQualityIssueRequest.class),
          new Resource("snapshots", CompanySnapshot.class, CompanySnapshotRequest.class),
          new Resource("snapshot-members", SnapshotMember.class, SnapshotMemberRequest.class),
          new Resource(
              "snapshot-financial-cells",
              SnapshotFinancialCell.class,
              SnapshotFinancialCellRequest.class),
          new Resource("officer-parties", OfficerParty.class, OfficerPartyRequest.class),
          new Resource(
              "officer-appointments", OfficerAppointment.class, OfficerAppointmentRequest.class),
          new Resource(
              "ownership-observations",
              OwnershipObservation.class,
              OwnershipObservationRequest.class),
          new Resource("debt-instruments", DebtInstrument.class, DebtInstrumentRequest.class),
          new Resource(
              "credit-rating-observations",
              CreditRatingObservation.class,
              CreditRatingObservationRequest.class),
          new Resource(
              "research-disclosures", ResearchDisclosure.class, ResearchDisclosureRequest.class),
          new Resource("reference-datasets", ReferenceDataset.class, ReferenceDatasetRequest.class),
          new Resource(
              "reference-dataset-observations",
              ReferenceDatasetObservation.class,
              ReferenceDatasetObservationRequest.class));
}

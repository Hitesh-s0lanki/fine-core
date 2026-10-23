package com.fincore.server.validation;

import com.fincore.server.config.Resources;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.company.CompanyIdentifier;
import com.fincore.server.models.company.CompanyProfileObservation;
import com.fincore.server.models.company.CompanyRelationship;
import com.fincore.server.models.financial.DerivedMetricObservation;
import com.fincore.server.models.financial.FinancialConcept;
import com.fincore.server.models.financial.FinancialObservation;
import com.fincore.server.models.governance.OwnershipObservation;
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
import com.fincore.server.repository.common.DomainRecordRepository;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.stereotype.Component;

/** Validates foreign identities and semantics before a record reaches PostgreSQL. */
@Component
public class RecordRules {
  private final DomainRecordRepository records;
  private final jakarta.persistence.EntityManager entityManager;
  private final com.fincore.server.repository.snapshot.SnapshotMemberRepository snapshotMembers;

  /** Uses persisted records as the authority for foreign-reference identity. */
  public RecordRules(
      DomainRecordRepository records,
      jakarta.persistence.EntityManager entityManager,
      com.fincore.server.repository.snapshot.SnapshotMemberRepository snapshotMembers) {
    this.records = records;
    this.entityManager = entityManager;
    this.snapshotMembers = snapshotMembers;
  }

  /** Enforces required evidence, cross-company isolation and structured field shapes. */
  public void validate(DomainRecord record) {
    for (UUID id : record.getEvidenceIds()) {
      require(id, SourceReference.class);
    }
    if (!isInfrastructure(record) && record.getEvidenceIds().isEmpty()) {
      throw new IllegalArgumentException("Company assertions require source evidence");
    }
    Set<UUID> companies = new HashSet<>();
    UUID provider = null;
    BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(record);
    if (bean.isReadableProperty("companyId")) {
      UUID companyId = (UUID) bean.getPropertyValue("companyId");
      if (companyId != null) {
        companies.add(companyId);
      }
    }
    if (bean.isReadableProperty("providerId")) {
      provider = (UUID) bean.getPropertyValue("providerId");
    }
    for (Field field : record.getClass().getDeclaredFields()) {
      JoinColumn join = field.getAnnotation(JoinColumn.class);
      if (join == null || !field.isAnnotationPresent(ManyToOne.class)) {
        continue;
      }
      String scalarName =
          Arrays.stream(record.getClass().getDeclaredFields())
              .filter(f -> f.isAnnotationPresent(Column.class))
              .filter(f -> f.getAnnotation(Column.class).name().equals(join.name()))
              .map(Field::getName)
              .findFirst()
              .orElseThrow();
      UUID id = (UUID) bean.getPropertyValue(scalarName);
      if (id == null) {
        continue;
      }
      DomainRecord target = require(id, field.getType());
      if (!(record instanceof CompanyRelationship)
          && !(record instanceof OwnershipObservation && scalarName.equals("holderCompanyId"))
          && !(record instanceof SnapshotMember)) {
        UUID owner = companyOf(target);
        if (owner != null) {
          companies.add(owner);
        }
      }
      if (target instanceof SourceEndpoint endpoint
          && provider != null
          && !provider.equals(endpoint.getProviderId())) {
        throw new IllegalArgumentException("Endpoint and provider do not match");
      }
      if (target instanceof ProviderEntityBinding binding
          && provider != null
          && !provider.equals(binding.getProviderId())) {
        throw new IllegalArgumentException("Entity binding and provider do not match");
      }
    }
    if (companies.size() > 1) {
      throw new IllegalArgumentException("References target different companies");
    }
    checkIntervals(bean);
    validateStructures(record, bean);
  }

  /** Loads a foreign reference and checks its actual canonical type. */
  public DomainRecord require(UUID id, Class<?> type) {
    Class<? extends DomainRecord> entityType = type.asSubclass(DomainRecord.class);
    if (entityType.equals(DomainRecord.class)) {
      String route =
          records
              .findResourceById(id)
              .orElseThrow(() -> new IllegalArgumentException("Unknown record reference"));
      entityType =
          Resources.ALL.stream()
              .filter(r -> r.route().equals(route))
              .map(Resources.Resource::entityType)
              .findFirst()
              .orElseThrow();
    }
    DomainRecord persisted = entityManager.find(entityType, id);
    if (persisted == null) {
      throw new IllegalArgumentException("Unknown record reference");
    }
    DomainRecord record = org.hibernate.Hibernate.unproxy(persisted, DomainRecord.class);
    if (!type.isInstance(record)) {
      throw new IllegalArgumentException("Reference has incorrect record type");
    }
    return record;
  }

  /** Resolves the subject issuer, including relationship and receipt-security parents. */
  public UUID companyOf(DomainRecord record) {
    if (record instanceof Company company) {
      return company.getId();
    }
    BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(record);
    if (bean.isReadableProperty("companyId")) {
      UUID companyId = (UUID) bean.getPropertyValue("companyId");
      if (companyId != null) {
        return companyId;
      }
    }
    if (record instanceof CompanyRelationship relationship) {
      return relationship.getSubjectCompanyId();
    }
    for (String property :
        List.of("securityId", "receiptSecurityId", "listingId", "registrationId", "filingId")) {
      if (bean.isReadableProperty(property)) {
        UUID id = (UUID) bean.getPropertyValue(property);
        if (id != null) {
          return companyOf(require(id, DomainRecord.class));
        }
      }
    }
    return null;
  }

  private boolean isInfrastructure(DomainRecord record) {
    return record instanceof SourceProvider
        || record instanceof SourceEndpoint
        || record instanceof RawArtifact
        || record instanceof IngestionAttempt
        || record instanceof NormalizationRun
        || record instanceof SourceReference
        || record instanceof CompanySnapshot
        || record instanceof SnapshotMember
        || record instanceof SnapshotFinancialCell;
  }

  @SuppressWarnings("unchecked")
  private void checkIntervals(BeanWrapper bean) {
    for (List<String> pair :
        List.of(
            List.of("validFrom", "validTo"),
            List.of("effectiveFrom", "effectiveTo"),
            List.of("periodStart", "periodEnd"),
            List.of("requestedAt", "retrievedAt"),
            List.of("startedAt", "finishedAt"),
            List.of("appointedOn", "resignedOn"),
            List.of("referencePeriodStart", "referencePeriodEnd"),
            List.of("realtimeStart", "realtimeEnd"))) {
      if (!bean.isReadableProperty(pair.getFirst()) || !bean.isReadableProperty(pair.getLast())) {
        continue;
      }
      Object start = bean.getPropertyValue(pair.getFirst());
      Object end = bean.getPropertyValue(pair.getLast());
      if (start != null && end != null && ((Comparable<Object>) start).compareTo(end) > 0) {
        throw new IllegalArgumentException("Reversed date interval");
      }
    }
  }

  private void validateStructures(DomainRecord record, BeanWrapper bean) {
    for (String name :
        List.of(
            "dimensions",
            "supportedDomains",
            "inputRecordRefs",
            "identifiers",
            "ingestionAttemptIds",
            "outputRecordIds",
            "qualityIssueIds")) {
      if (bean.isReadableProperty(name) && !(bean.getPropertyValue(name) instanceof List<?>)) {
        throw new IllegalArgumentException(name + " must be an array");
      }
    }
    if (bean.isReadableProperty("dimensions")) {
      Set<String> axes = new HashSet<>();
      for (Object raw : (List<?>) bean.getPropertyValue("dimensions")) {
        if (!(raw instanceof Map<?, ?> dimension)
            || !(dimension.get("axis") instanceof String axis)
            || !axes.add(axis)) {
          throw new IllegalArgumentException("Invalid or duplicate dimension axis");
        }
        String kind = Objects.toString(dimension.get("kind"), "explicit");
        String valueKey = kind.equals("typed") ? "typedValueCanonicalXml" : "member";
        if (!Set.of("explicit", "typed").contains(kind)
            || !(dimension.get(valueKey) instanceof String value)
            || value.isBlank()
            || !Set.of("axis", "kind", valueKey).containsAll(dimension.keySet())) {
          throw new IllegalArgumentException("Invalid financial dimension");
        }
      }
    }
    for (String field : List.of("sha256", "originalResponseSha256", "adapterContentHash")) {
      if (bean.isReadableProperty(field)
          && bean.getPropertyValue(field) != null
          && !bean.getPropertyValue(field).toString().matches("[a-f0-9]{64}")) {
        throw new IllegalArgumentException("Invalid SHA256 hash");
      }
    }
    if (record instanceof Company company && !company.getSlug().matches("[a-z][a-z0-9-]{0,99}")) {
      throw new IllegalArgumentException("Invalid company slug");
    }
    if (record instanceof CompanyIdentifier identifier
        && identifier.getScheme().equals("cik")
        && !identifier.getValue().matches("[0-9]{10}")) {
      throw new IllegalArgumentException("CIK must have ten digits");
    }
    if (record instanceof CompanyProfileObservation profile) {
      validateProfile(profile);
    }
    if (record instanceof FinancialObservation fact) {
      if ((fact.getPeriodType().equals("duration") && fact.getPeriodStart() == null)
          || (fact.getPeriodType().equals("instant") && fact.getPeriodStart() != null)) {
        throw new IllegalArgumentException("Financial period semantics do not match dates");
      }
      FinancialConcept concept =
          (FinancialConcept) require(fact.getConceptId(), FinancialConcept.class);
      if (!concept.getPeriodType().equals("unknown")
          && !concept.getPeriodType().equals(fact.getPeriodType())) {
        throw new IllegalArgumentException("Concept and fact period type mismatch");
      }
    }
    if (record instanceof DerivedMetricObservation derived) {
      validateDerived(derived);
    }
  }

  private void validateDerived(DerivedMetricObservation derived) {
    List<?> inputs = (List<?>) derived.getInputRecordRefs();
    if (inputs.isEmpty()) {
      throw new IllegalArgumentException("Derived metrics require inputs");
    }
    for (Object input : inputs) {
      if (!(input instanceof Map<?, ?> ref)
          || !ref.keySet().equals(Set.of("recordType", "recordId"))) {
        throw new IllegalArgumentException("Invalid derived input reference");
      }
      UUID inputId = UUID.fromString(ref.get("recordId").toString());
      DomainRecord target = require(inputId, DomainRecord.class);
      UUID owner = companyOf(target);
      if (owner != null && !derived.getCompanyId().equals(owner)) {
        throw new IllegalArgumentException("Derived input belongs to a different company");
      }
      if (!snapshotMembers.existsBySnapshotIdAndRecordId(derived.getSnapshotId(), inputId)) {
        throw new IllegalArgumentException("Derived input must belong to the input snapshot");
      }
      if (!Resources.ALL.stream()
          .anyMatch(
              r -> r.route().equals(ref.get("recordType")) && r.entityType().isInstance(target))) {
        throw new IllegalArgumentException("Derived input record type mismatch");
      }
    }
  }

  private void validateProfile(CompanyProfileObservation profile) {
    Object value = profile.getProfileValue();
    String field = profile.getField();
    Set<String> structured =
        Set.of(
            "address",
            "industry_classification",
            "sector_classification",
            "fiscal_year_end",
            "employee_count",
            "executive_summary");
    if (!structured.contains(field)) {
      if (!(value instanceof String text) || text.isBlank()) {
        throw new IllegalArgumentException("Profile field requires a nonblank string");
      }
      if (Set.of("domicile_country", "incorporation_country").contains(field)
          && !value.toString().matches("[A-Z]{2}")) {
        throw new IllegalArgumentException("Expected country code");
      }
      if (field.equals("reporting_currency") && !value.toString().matches("[A-Z]{3}")) {
        throw new IllegalArgumentException("Expected currency code");
      }
      return;
    }
    if (!(value instanceof Map<?, ?> map)) {
      throw new IllegalArgumentException("Profile field requires an object");
    }
    Set<String> allowed =
        switch (field) {
          case "address" ->
              Set.of(
                  "kind",
                  "line1",
                  "line2",
                  "city",
                  "region",
                  "postalCode",
                  "countryCode",
                  "rawText");
          case "industry_classification", "sector_classification" ->
              Set.of("scheme", "code", "label", "authority");
          case "fiscal_year_end" -> Set.of("month", "day");
          case "employee_count" -> Set.of("count", "basis", "asOfDate");
          default -> Set.of("name", "role", "personId", "asOfDate");
        };
    if (!allowed.containsAll(map.keySet())) {
      throw new IllegalArgumentException("Unknown profile value field");
    }
    switch (field) {
      case "fiscal_year_end" -> {
        LocalDate.of(2000, integer(map.get("month")), integer(map.get("day")));
      }
      case "employee_count" -> {
        nonblank(map.get("basis"));
        if (integer(map.get("count")) < 0
            || !Set.of("full_time", "total", "unknown").contains(map.get("basis"))) {
          throw new IllegalArgumentException("Invalid employee count");
        }
      }
      case "address" -> {
        nonblank(map.get("kind"));
        if (!Set.of("registered", "business", "mailing", "overseas", "other")
            .contains(map.get("kind"))) {
          throw new IllegalArgumentException("Invalid address kind");
        }
        nonblank(map.get("rawText"));
      }
      case "industry_classification", "sector_classification" -> {
        for (String key : List.of("scheme", "label", "authority")) {
          nonblank(map.get(key));
        }
      }
      default -> {
        nonblank(map.get("name"));
        nonblank(map.get("role"));
      }
    }
  }

  private int integer(Object value) {
    if (!(value instanceof Integer integer)) {
      throw new IllegalArgumentException("Expected an integer");
    }
    return integer;
  }

  private void nonblank(Object value) {
    if (!(value instanceof String text) || text.isBlank()) {
      throw new IllegalArgumentException("Missing text value");
    }
  }
}

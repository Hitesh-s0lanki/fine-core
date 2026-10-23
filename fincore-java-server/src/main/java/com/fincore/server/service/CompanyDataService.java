package com.fincore.server.service;

import com.fincore.server.config.Resources;
import com.fincore.server.dto.common.RecordPage;
import com.fincore.server.dto.common.RecordView;
import com.fincore.server.interfaces.CompanyDataServiceInter;
import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.validation.RecordRules;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Transactional creation and bounded reads across the allowlisted company schemas. */
@Service
@Transactional(readOnly = true)
public class CompanyDataService implements CompanyDataServiceInter {
  private static final Set<String> SNAPSHOT_RESOURCES =
      Set.of("snapshots", "snapshot-members", "snapshot-financial-cells");
  private static final Set<String> FILTERS =
      Set.of(
          "companyId",
          "securityId",
          "listingId",
          "filingId",
          "snapshotId",
          "seriesId",
          "metricId",
          "providerId",
          "scope",
          "periodEnd");
  private final Repositories repositories;
  private final ObjectMapper mapper;
  private final Validator validator;
  private final RecordRules rules;

  /** Wires Spring Data repositories, strict DTO conversion and semantic validation. */
  public CompanyDataService(
      ApplicationContext context, ObjectMapper mapper, Validator validator, RecordRules rules) {
    this.repositories = new Repositories(context);
    this.mapper = mapper;
    this.validator = validator;
    this.rules = rules;
  }

  /** Creates a provider/domain assertion using its exact, allowlisted request DTO. */
  @Transactional
  public RecordView create(String route, JsonNode input) {
    if (SNAPSHOT_RESOURCES.contains(route) || route.equals("companies")) {
      throw new IllegalArgumentException("Use the company onboarding or snapshot endpoint");
    }
    Resources.Resource resource = resource(route);
    CreationRequest request = mapper.treeToValue(input, resource.requestType());
    return view(save(resource, request));
  }

  /** Validates and persists an internal typed request in the caller's transaction. */
  @Transactional
  public DomainRecord save(Resources.Resource resource, CreationRequest request) {
    var violations = validator.validate(request);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
    if (!resource.requestType().isInstance(request)) {
      throw new IllegalArgumentException("Wrong request type for resource");
    }
    DomainRecord record = request.toEntity();
    rules.validate(record);
    var properties = PropertyAccessorFactory.forBeanPropertyAccess(record);
    if (properties.isReadableProperty("assertionKey")) {
      var existing =
          queries(resource)
              .findAll(
                  (root, query, builder) ->
                      builder.equal(
                          root.get("assertionKey"), properties.getPropertyValue("assertionKey")),
                  PageRequest.of(0, 1));
      if (!existing.isEmpty()) {
        DomainRecord prior = existing.getContent().getFirst();
        if (!view(prior).attributes().equals(view(record).attributes())
            || !prior.getEvidenceIds().equals(record.getEvidenceIds())) {
          throw new ResponseStatusException(
              HttpStatus.CONFLICT, "Assertion key reused with different content");
        }
        return prior;
      }
    }
    return repository(resource).saveAndFlush(record);
  }

  /** Returns a single assertion after checking both UUID and canonical resource type. */
  public RecordView get(String route, UUID id) {
    return view(load(route, id));
  }

  /** Loads a typed canonical assertion or returns a not-found response. */
  public DomainRecord load(String route, UUID id) {
    return repository(resource(route))
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
  }

  /** Lists records with a bounded page and allowlisted, typed equality filters. */
  public RecordPage list(String route, int page, int size, Map<String, String> filters) {
    if (page < 0 || size < 1 || size > 100 || !FILTERS.containsAll(filters.keySet())) {
      throw new IllegalArgumentException("Invalid pagination or filter");
    }
    Resources.Resource resource = resource(route);
    var resourceProperties = PropertyAccessorFactory.forBeanPropertyAccess(newEntity(resource));
    for (String field : filters.keySet()) {
      if (!resourceProperties.isReadableProperty(field)
          && !(field.equals("companyId") && route.equals("listings"))) {
        throw new IllegalArgumentException("Filter is not supported by this resource");
      }
    }
    Specification<DomainRecord> specification =
        (root, query, builder) -> {
          var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
          for (var filter : filters.entrySet()) {
            if (filter.getKey().equals("companyId")
                && !PropertyAccessorFactory.forBeanPropertyAccess(newEntity(resource))
                    .isReadableProperty("companyId")) {
              if (route.equals("listings")) {
                predicates.add(
                    builder.equal(
                        root.join("securityReference").get("companyId"),
                        UUID.fromString(filter.getValue())));
                continue;
              }
              throw new IllegalArgumentException("Resource has no direct company filter");
            }
            var path = root.get(filter.getKey());
            Object value =
                switch (filter.getKey()) {
                  case "scope" -> filter.getValue();
                  case "periodEnd" -> java.time.LocalDate.parse(filter.getValue());
                  default -> UUID.fromString(filter.getValue());
                };
            predicates.add(builder.equal(path, value));
          }
          return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    Page<DomainRecord> result =
        queries(resource)
            .findAll(specification, PageRequest.of(page, size, Sort.by("createdAt", "id")));
    return new RecordPage(
        result.map(this::view).getContent(),
        page,
        size,
        result.getTotalElements(),
        result.hasNext());
  }

  /** Converts a managed record to a detached API representation with exact decimal strings. */
  public RecordView view(DomainRecord record) {
    @SuppressWarnings("unchecked")
    Map<String, Object> fields = new LinkedHashMap<>(mapper.convertValue(record, Map.class));
    fields.keySet().removeAll(Set.of("id", "schemaVersion", "createdAt", "evidenceIds"));
    String route =
        Resources.ALL.stream()
            .filter(r -> r.entityType().isInstance(record))
            .map(Resources.Resource::route)
            .findFirst()
            .orElseThrow();
    return new RecordView(
        record.getId(),
        route,
        record.getSchemaVersion(),
        record.getCreatedAt(),
        Set.copyOf(record.getEvidenceIds()),
        fields);
  }

  /** Resolves only resources explicitly registered in this server. */
  public Resources.Resource resource(String route) {
    return Resources.ALL.stream()
        .filter(r -> r.route().equals(route))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown resource"));
  }

  @SuppressWarnings("unchecked")
  private JpaRepository<DomainRecord, UUID> repository(Resources.Resource resource) {
    return (JpaRepository<DomainRecord, UUID>)
        repositories.getRepositoryFor(resource.entityType()).orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private JpaSpecificationExecutor<DomainRecord> queries(Resources.Resource resource) {
    return (JpaSpecificationExecutor<DomainRecord>)
        repositories.getRepositoryFor(resource.entityType()).orElseThrow();
  }

  private DomainRecord newEntity(Resources.Resource resource) {
    try {
      return resource.entityType().getConstructor().newInstance();
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}

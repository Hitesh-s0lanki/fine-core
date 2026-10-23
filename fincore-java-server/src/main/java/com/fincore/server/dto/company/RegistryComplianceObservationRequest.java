package com.fincore.server.dto.company;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.RegistryComplianceObservation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for registry compliance observation. */
public record RegistryComplianceObservationRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID registrationId,
    LocalDate asOfDate,
    LocalDate dissolutionDate,
    LocalDate accountsLastMadeUpTo,
    LocalDate accountsNextDue,
    Boolean accountsOverdue,
    LocalDate confirmationStatementLastMadeUpTo,
    LocalDate confirmationStatementNextDue,
    Boolean confirmationStatementOverdue,
    @Size(max = 40000) String partialDataAvailable)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    RegistryComplianceObservation entity = new RegistryComplianceObservation();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setRegistrationId(registrationId);
    entity.setAsOfDate(asOfDate);
    entity.setDissolutionDate(dissolutionDate);
    entity.setAccountsLastMadeUpTo(accountsLastMadeUpTo);
    entity.setAccountsNextDue(accountsNextDue);
    entity.setAccountsOverdue(accountsOverdue);
    entity.setConfirmationStatementLastMadeUpTo(confirmationStatementLastMadeUpTo);
    entity.setConfirmationStatementNextDue(confirmationStatementNextDue);
    entity.setConfirmationStatementOverdue(confirmationStatementOverdue);
    entity.setPartialDataAvailable(partialDataAvailable);
    return entity;
  }
}

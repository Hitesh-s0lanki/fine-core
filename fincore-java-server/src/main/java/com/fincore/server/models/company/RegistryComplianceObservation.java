package com.fincore.server.models.company;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Persistent registry compliance observation assertion. */
@Entity
@Table(name = "registry_compliance_observation")
@DiscriminatorValue("registry-compliance")
@Getter
@Setter
@NoArgsConstructor
public class RegistryComplianceObservation extends DomainRecord {
  @Column(name = "registration_id", nullable = false)
  private UUID registrationId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registration_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private CompanyRegistration registrationReference;

  @Column(name = "as_of_date", nullable = true)
  private LocalDate asOfDate;

  @Column(name = "dissolution_date", nullable = true)
  private LocalDate dissolutionDate;

  @Column(name = "accounts_last_made_up_to", nullable = true)
  private LocalDate accountsLastMadeUpTo;

  @Column(name = "accounts_next_due", nullable = true)
  private LocalDate accountsNextDue;

  @Column(name = "accounts_overdue", nullable = true)
  private Boolean accountsOverdue;

  @Column(name = "confirmation_statement_last_made_up_to", nullable = true)
  private LocalDate confirmationStatementLastMadeUpTo;

  @Column(name = "confirmation_statement_next_due", nullable = true)
  private LocalDate confirmationStatementNextDue;

  @Column(name = "confirmation_statement_overdue", nullable = true)
  private Boolean confirmationStatementOverdue;

  @Column(name = "partial_data_available", nullable = true)
  private String partialDataAvailable;
}

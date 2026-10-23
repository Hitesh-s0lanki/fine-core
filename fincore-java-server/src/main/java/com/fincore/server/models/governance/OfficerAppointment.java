package com.fincore.server.models.governance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.company.Company;
import com.fincore.server.models.company.CompanyRegistration;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistent officer appointment assertion. */
@Entity
@Table(name = "officer_appointment")
@DiscriminatorValue("officer-appointments")
@Getter
@Setter
@NoArgsConstructor
public class OfficerAppointment extends DomainRecord {
  @Column(name = "company_id", nullable = false)
  private UUID companyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private Company companyReference;

  @Column(name = "registration_id", nullable = false)
  private UUID registrationId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "registration_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private CompanyRegistration registrationReference;

  @Column(name = "officer_party_id", nullable = false)
  private UUID officerPartyId;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "officer_party_id", insertable = false, updatable = false)
  @Setter(lombok.AccessLevel.NONE)
  private OfficerParty officerPartyReference;

  @Column(name = "source_appointment_id", nullable = false)
  private String sourceAppointmentId;

  @Column(name = "role", nullable = false)
  private String role;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "service_address", nullable = false, columnDefinition = "jsonb")
  private Object serviceAddress;

  @Column(name = "appointed_on", nullable = true)
  private LocalDate appointedOn;

  @Column(name = "resigned_on", nullable = true)
  private LocalDate resignedOn;

  @Column(name = "is_pre1992_appointment", nullable = true)
  private Boolean isPre1992Appointment;

  @Column(name = "birth_month", nullable = true)
  private Integer birthMonth;

  @Column(name = "birth_year", nullable = true)
  private Integer birthYear;
}

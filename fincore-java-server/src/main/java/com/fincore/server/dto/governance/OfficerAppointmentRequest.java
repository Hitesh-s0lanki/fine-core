package com.fincore.server.dto.governance;

import com.fincore.server.interfaces.CreationRequest;
import com.fincore.server.models.common.DomainRecord;
import com.fincore.server.models.governance.OfficerAppointment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Validated creation request for officer appointment. */
public record OfficerAppointmentRequest(
    @NotNull Set<@NotNull UUID> evidenceIds,
    @NotNull UUID companyId,
    @NotNull UUID registrationId,
    @NotNull UUID officerPartyId,
    @NotBlank @Size(max = 40000) String sourceAppointmentId,
    @NotBlank @Size(max = 40000) String role,
    @NotNull Object serviceAddress,
    LocalDate appointedOn,
    LocalDate resignedOn,
    Boolean isPre1992Appointment,
    Integer birthMonth,
    Integer birthYear)
    implements CreationRequest {
  @Override
  public DomainRecord toEntity() {
    OfficerAppointment entity = new OfficerAppointment();
    entity.setEvidenceIds(new java.util.LinkedHashSet<>(evidenceIds));
    entity.setCompanyId(companyId);
    entity.setRegistrationId(registrationId);
    entity.setOfficerPartyId(officerPartyId);
    entity.setSourceAppointmentId(sourceAppointmentId);
    entity.setRole(role);
    entity.setServiceAddress(serviceAddress);
    entity.setAppointedOn(appointedOn);
    entity.setResignedOn(resignedOn);
    entity.setIsPre1992Appointment(isPre1992Appointment);
    entity.setBirthMonth(birthMonth);
    entity.setBirthYear(birthYear);
    return entity;
  }
}

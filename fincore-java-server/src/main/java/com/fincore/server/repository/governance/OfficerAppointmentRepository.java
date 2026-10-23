package com.fincore.server.repository.governance;

import com.fincore.server.models.governance.OfficerAppointment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for officer appointment records. */
public interface OfficerAppointmentRepository
    extends JpaRepository<OfficerAppointment, UUID>, JpaSpecificationExecutor<OfficerAppointment> {}

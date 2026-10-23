package com.fincore.server.repository.disclosure;

import com.fincore.server.models.disclosure.Announcement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for announcement records. */
public interface AnnouncementRepository
    extends JpaRepository<Announcement, UUID>, JpaSpecificationExecutor<Announcement> {}

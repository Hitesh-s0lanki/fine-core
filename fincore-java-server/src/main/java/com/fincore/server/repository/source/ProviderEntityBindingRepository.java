package com.fincore.server.repository.source;

import com.fincore.server.models.source.ProviderEntityBinding;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Persistence and bounded query access for provider entity binding records. */
public interface ProviderEntityBindingRepository
    extends JpaRepository<ProviderEntityBinding, UUID>,
        JpaSpecificationExecutor<ProviderEntityBinding> {}

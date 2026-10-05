package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Pool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PoolRepository extends JpaRepository<Pool, UUID> {
    Page<Pool> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);
    List<Pool> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
}

package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.SwimClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SwimClassRepository extends JpaRepository<SwimClass, UUID> {
    Page<SwimClass> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);
    List<SwimClass> findByTenantIdAndDeletedAtIsNull(UUID tenantId);
}

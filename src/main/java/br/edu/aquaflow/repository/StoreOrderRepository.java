package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.StoreOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, UUID> {
    @EntityGraph(attributePaths = "items")
    List<StoreOrder> findByTenantIdAndStudentIdOrderByCreatedAtDesc(UUID tenantId, UUID studentId);

    @EntityGraph(attributePaths = "items")
    Optional<StoreOrder> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = {"items", "student"})
    List<StoreOrder> findTop5ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}

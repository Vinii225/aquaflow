package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Plan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    @EntityGraph(attributePaths = "features")
    List<Plan> findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(UUID tenantId);
}

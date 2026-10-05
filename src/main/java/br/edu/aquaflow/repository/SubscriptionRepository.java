package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Subscription;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Page<Subscription> findByTenantId(UUID tenantId, Pageable pageable);
    List<Subscription> findByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);
    List<Subscription> findByStudentId(UUID studentId);
}

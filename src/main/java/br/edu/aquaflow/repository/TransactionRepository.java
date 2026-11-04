package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Transaction;
import br.edu.aquaflow.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByTenantIdOrderByPaidAtDesc(UUID tenantId, Pageable pageable);

    @Query("select t from Transaction t where t.tenant.id = :tenantId and t.type = :type and t.paidAt >= :from")
    List<Transaction> findByTypeSince(UUID tenantId, TransactionType type, Instant from);
}

package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Invoice;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByTenantIdOrderByDueDateDesc(UUID tenantId, Pageable pageable);
    List<Invoice> findBySubscriptionIdOrderByReferenceMonthDesc(UUID subscriptionId);
    Optional<Invoice> findBySubscriptionIdAndReferenceMonth(UUID subscriptionId, LocalDate referenceMonth);

    @org.springframework.data.jpa.repository.Query(
        "select i from Invoice i where i.subscription.student.id = :studentId order by i.referenceMonth desc")
    List<Invoice> findByStudentId(UUID studentId);

    long countByTenantIdAndStatus(UUID tenantId, InvoiceStatus status);

    long countByTenantIdAndStatusAndDueDateBefore(UUID tenantId, InvoiceStatus status, LocalDate date);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(i.amount), 0) from Invoice i where i.tenant.id = :tenantId and i.status = :status")
    BigDecimal sumAmountByStatus(UUID tenantId, InvoiceStatus status);
}

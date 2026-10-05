package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

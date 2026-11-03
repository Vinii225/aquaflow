package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.ContactMessage;
import br.edu.aquaflow.domain.enums.ContactMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessageStatus status, Pageable pageable);
    long countByStatus(ContactMessageStatus status);
}

package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.StudentBooking;
import br.edu.aquaflow.domain.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentBookingRepository extends JpaRepository<StudentBooking, UUID> {
    List<StudentBooking> findByStudentIdOrderByBookedAtDesc(UUID studentId);
    List<StudentBooking> findByClassSessionIdAndStatus(UUID classSessionId, BookingStatus status);
    long countByClassSessionIdAndStatus(UUID classSessionId, BookingStatus status);
    Optional<StudentBooking> findByStudentIdAndClassSessionId(UUID studentId, UUID classSessionId);
}

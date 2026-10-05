package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByStudentBookingId(UUID studentBookingId);
}

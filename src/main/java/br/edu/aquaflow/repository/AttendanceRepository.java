package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    Optional<Attendance> findByStudentBookingId(UUID studentBookingId);

    /** Presenças registradas pelo professor desde a data: cada linha é [status, quantidade]. */
    @Query("select a.status, count(a) from Attendance a " +
           "where a.studentBooking.classSession.teacher.id = :teacherId " +
           "and a.studentBooking.classSession.date >= :from group by a.status")
    List<Object[]> countByStatusForTeacherSince(UUID teacherId, LocalDate from);
}

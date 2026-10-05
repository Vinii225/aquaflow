package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.ClassSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {
    Page<ClassSession> findByTenantIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
            UUID tenantId, LocalDate from, Pageable pageable);

    List<ClassSession> findByTeacherIdAndDateOrderByStartTimeAsc(UUID teacherId, LocalDate date);

    List<ClassSession> findByTeacherIdAndDateBetweenOrderByDateAscStartTimeAsc(
            UUID teacherId, LocalDate from, LocalDate to);

    Optional<ClassSession> findByClassScheduleIdAndDate(UUID classScheduleId, LocalDate date);
}

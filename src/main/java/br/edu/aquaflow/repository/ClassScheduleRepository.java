package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.ClassSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, UUID> {
    Page<ClassSchedule> findByTenantId(UUID tenantId, Pageable pageable);
    List<ClassSchedule> findByTenantId(UUID tenantId);
    List<ClassSchedule> findByTeacherId(UUID teacherId);
}

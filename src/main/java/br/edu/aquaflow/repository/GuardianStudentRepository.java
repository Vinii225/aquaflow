package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.GuardianStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuardianStudentRepository extends JpaRepository<GuardianStudent, UUID> {
    List<GuardianStudent> findByGuardianId(UUID guardianId);
}

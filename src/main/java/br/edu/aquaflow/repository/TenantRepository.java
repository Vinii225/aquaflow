package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findFirstByDefaultSchoolTrue();
    Optional<Tenant> findFirstByOrderByCreatedAtAsc();

    /** Escolas cadastradas na plataforma: a padrão primeiro, depois por data de cadastro. */
    List<Tenant> findByDeletedAtIsNullOrderByDefaultSchoolDescCreatedAtAsc();
}

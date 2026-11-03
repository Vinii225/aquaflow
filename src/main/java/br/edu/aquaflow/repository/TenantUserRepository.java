package br.edu.aquaflow.repository;

import br.edu.aquaflow.domain.TenantUser;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantUserRepository extends JpaRepository<TenantUser, UUID> {
    List<TenantUser> findByUserId(UUID userId);
    List<TenantUser> findByTenantIdAndRole(UUID tenantId, TenantUserRole role);
    List<TenantUser> findByTenantId(UUID tenantId);
    Optional<TenantUser> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /** Usuários com vínculo ativo na escola para o papel informado (ex.: professores para montar a grade). */
    default List<User> findActiveUsersByRole(UUID tenantId, TenantUserRole role) {
        return findByTenantIdAndRole(tenantId, role).stream()
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .map(TenantUser::getUser)
                .toList();
    }
}

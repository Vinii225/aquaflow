package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.repository.TenantRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Resolve a escola (tenant) da requisição, num só lugar:
 * <ol>
 *   <li>escola forçada com {@link #callAs} (webhook de pagamento, testes);</li>
 *   <li>usuário logado: escola do vínculo em {@code tenant_users}, guardada no login;</li>
 *   <li>visitante anônimo: escola padrão ({@code tenants.default_school}).</li>
 * </ol>
 * Toda consulta de dados da escola deve filtrar por {@link #getTenantId()}.
 */
@Service
public class TenantContext {

    private static final ThreadLocal<UUID> FORCED = new ThreadLocal<>();

    private final TenantRepository tenantRepository;
    private volatile UUID defaultTenantId;

    public TenantContext(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant getTenant() {
        UUID tenantId = getTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Escola não encontrada: " + tenantId));
    }

    public UUID getTenantId() {
        UUID forced = FORCED.get();
        if (forced != null) {
            return forced;
        }
        return loggedUserTenantId().orElseGet(this::getDefaultTenantId);
    }

    /** Escola do site público. Sem nenhuma marcada como padrão, vale a mais antiga. */
    public UUID getDefaultTenantId() {
        UUID id = defaultTenantId;
        if (id == null) {
            id = tenantRepository.findFirstByDefaultSchoolTrue()
                    .or(tenantRepository::findFirstByOrderByCreatedAtAsc)
                    .map(Tenant::getId)
                    .orElseThrow(() -> new IllegalStateException("Nenhuma escola (tenant) cadastrada"));
            defaultTenantId = id;
        }
        return id;
    }

    /** Executa {@code action} como se a requisição fosse da escola informada. */
    public <T> T callAs(UUID tenantId, Supplier<T> action) {
        UUID previous = FORCED.get();
        FORCED.set(tenantId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                FORCED.remove();
            } else {
                FORCED.set(previous);
            }
        }
    }

    public void runAs(UUID tenantId, Runnable action) {
        callAs(tenantId, () -> {
            action.run();
            return null;
        });
    }

    private static Optional<UUID> loggedUserTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AquaflowUserDetails user) {
            return Optional.ofNullable(user.getTenantId());
        }
        return Optional.empty();
    }
}

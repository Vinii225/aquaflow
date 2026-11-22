package br.edu.aquaflow.security;

import br.edu.aquaflow.domain.TenantUser;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.TenantStatus;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.repository.UserRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AquaflowUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantContext tenantContext;

    public AquaflowUserDetailsService(UserRepository userRepository,
                                       TenantUserRepository tenantUserRepository,
                                       TenantContext tenantContext) {
        this.userRepository = userRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.tenantContext = tenantContext;
    }

    /**
     * A escola da sessão é a do vínculo ativo do usuário. Multi-escolas fase 1: com vínculo
     * em mais de uma escola, vale o mais antigo (escolher a escola no login fica para a Sprint #5).
     * A Equipe AquaFlow (plataforma) não tem vínculo e fica na escola padrão.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        List<TenantUser> links = tenantUserRepository.findByUserId(user.getId()).stream()
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .filter(tu -> tu.getTenant().getStatus() == TenantStatus.ACTIVE)
                .toList();

        UUID linkedTenantId = links.stream()
                .min(Comparator.comparing(TenantUser::getCreatedAt))
                .map(tu -> tu.getTenant().getId())
                .orElse(null);
        UUID tenantId = linkedTenantId;

        Set<String> roles = links.stream()
                .filter(tu -> tu.getTenant().getId().equals(linkedTenantId))
                .map(TenantUser::getRole)
                .map(Enum::name)
                .collect(Collectors.toCollection(HashSet::new));

        if (user.isPlatformAdmin()) {
            roles.add("PLATFORM_ADMIN");
            if (tenantId == null) {
                tenantId = tenantContext.getDefaultTenantId();
            }
        }

        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("Usuário sem vínculo ativo com nenhuma escola: " + email);
        }

        return new AquaflowUserDetails(user, tenantId, roles);
    }
}

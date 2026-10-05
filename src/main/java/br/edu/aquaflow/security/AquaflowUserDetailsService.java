package br.edu.aquaflow.security;

import br.edu.aquaflow.domain.TenantUser;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.repository.UserRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
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

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));

        var tenantId = tenantContext.getTenantId();

        Set<String> roles = tenantUserRepository.findByUserId(user.getId()).stream()
                .filter(tu -> tu.getTenant().getId().equals(tenantId))
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .map(TenantUser::getRole)
                .map(Enum::name)
                .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            throw new UsernameNotFoundException("Usuário sem vínculo ativo com a escola: " + email);
        }

        return new AquaflowUserDetails(user, roles);
    }
}

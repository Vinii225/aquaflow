package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.TenantUser;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantContext tenantContext;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                        TenantUserRepository tenantUserRepository,
                        TenantContext tenantContext,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.tenantContext = tenantContext;
        this.passwordEncoder = passwordEncoder;
    }

    public static class EmailAlreadyUsedException extends RuntimeException {
        public EmailAlreadyUsedException(String email) {
            super("Já existe uma conta com o e-mail " + email);
        }
    }

    @Transactional
    public User registerStudent(String name, String email, String rawPassword) {
        return registerWithRole(name, email, rawPassword, TenantUserRole.STUDENT);
    }

    @Transactional
    public User registerWithRole(String name, String email, String rawPassword, TenantUserRole role) {
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);

        TenantUser tenantUser = new TenantUser();
        tenantUser.setTenant(tenantContext.getTenant());
        tenantUser.setUser(user);
        tenantUser.setRole(role);
        tenantUser.setStatus(MembershipStatus.ACTIVE);
        tenantUserRepository.save(tenantUser);

        return user;
    }
}

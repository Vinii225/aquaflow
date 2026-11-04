package br.edu.aquaflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/planos/*/contratar", "/planos/contratacao/**",
                        "/loja/checkout", "/loja/pedidos/**").hasRole("STUDENT")
                .requestMatchers("/", "/planos", "/planos/contato", "/loja", "/loja/carrinho", "/loja/carrinho/**", "/login", "/register", "/css/**", "/img/**", "/webjars/**", "/error").permitAll()
                .requestMatchers("/plataforma/**").hasRole("PLATFORM_ADMIN")
                .requestMatchers("/admin/**").hasRole("SCHOOL_ADMIN")
                .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "SCHOOL_ADMIN")
                .requestMatchers("/student/orders").hasRole("STUDENT")
                .requestMatchers("/student/**").hasAnyRole("STUDENT", "GUARDIAN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                // Volta para a página que pediu o login (ex.: contratação de plano); sem ela, vai para o início
                .defaultSuccessUrl("/", false)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf.csrfTokenRequestHandler(eagerCsrfHandler()));

        return http.build();
    }

    // Carrega o token CSRF no início da requisição: páginas públicas grandes com formulário
    // (ex.: modal de /planos) quebravam para visitantes sem sessão, pois o token era criado
    // depois de a resposta já ter sido enviada.
    private XorCsrfTokenRequestAttributeHandler eagerCsrfHandler() {
        var handler = new XorCsrfTokenRequestAttributeHandler();
        handler.setCsrfRequestAttributeName(null);
        return handler;
    }
}

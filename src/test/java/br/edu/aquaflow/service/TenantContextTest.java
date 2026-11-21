package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextTest {

    private final TenantRepository repository = mock(TenantRepository.class);
    private final TenantContext context = new TenantContext(repository);

    @AfterEach
    void limpa() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonimoUsaAEscolaPadrao() {
        Tenant padrao = tenant();
        when(repository.findFirstByDefaultSchoolTrue()).thenReturn(Optional.of(padrao));

        assertThat(context.getTenantId()).isEqualTo(padrao.getId());
    }

    @Test
    void comUmaEscolaSoESemPadraoMarcadoUsaAMaisAntiga() {
        Tenant unica = tenant();
        when(repository.findFirstByDefaultSchoolTrue()).thenReturn(Optional.empty());
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(unica));

        assertThat(context.getTenantId()).isEqualTo(unica.getId());
    }

    @Test
    void escolaForcadaValeSoDentroDoBloco() {
        Tenant padrao = tenant();
        UUID outra = UUID.randomUUID();
        when(repository.findFirstByDefaultSchoolTrue()).thenReturn(Optional.of(padrao));

        assertThat(context.callAs(outra, context::getTenantId)).isEqualTo(outra);
        assertThat(context.getTenantId()).isEqualTo(padrao.getId());
    }

    private static Tenant tenant() {
        Tenant t = new Tenant();
        t.setId(UUID.randomUUID());
        return t;
    }
}

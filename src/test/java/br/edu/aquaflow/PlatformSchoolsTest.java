package br.edu.aquaflow;

import br.edu.aquaflow.service.platform.SchoolOverviewService;
import br.edu.aquaflow.service.platform.SchoolOverviewService.SchoolSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tela "Escolas" da Equipe AquaFlow: mostra as duas escolas de demonstração, cada uma com os próprios números. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PlatformSchoolsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired SchoolOverviewService schoolOverviewService;

    @Test
    void equipeAquaflowVeAsDuasEscolas() throws Exception {
        mvc.perform(get("/plataforma/escolas").session(login("plataforma@aquaflow.com", "plataforma123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escolas na plataforma")))
                .andExpect(content().string(containsString("AquaFlow Escola de Natação")))
                .andExpect(content().string(containsString("Av. das Piscinas, 123 - Centro")))
                .andExpect(content().string(containsString("admin@aquaflow.com")))
                .andExpect(content().string(containsString("Golfinhos Natação")))
                .andExpect(content().string(containsString("Rua das Ondas, 456 - Bairro Novo")))
                .andExpect(content().string(containsString("admin@golfinhos.com")))
                .andExpect(content().string(containsString("Escola padrão")));
    }

    @Test
    void cadaEscolaTemOsPropriosNumeros() {
        List<SchoolSummary> schools = schoolOverviewService.overview().schools();

        assertThat(schools).extracting(s -> s.school().getSlug()).containsExactly("aquaflow", "golfinhos");

        SchoolSummary golfinhos = schools.get(1);
        assertThat(golfinhos.students()).isEqualTo(1);
        assertThat(golfinhos.teachers()).isEqualTo(1);
        assertThat(golfinhos.pools()).isEqualTo(1);
        assertThat(golfinhos.classes()).isEqualTo(1);
        assertThat(golfinhos.plans()).isEqualTo(2);
        assertThat(golfinhos.products()).isEqualTo(3);
        assertThat(golfinhos.activeSubscriptions()).isEqualTo(1);
        assertThat(golfinhos.openInvoices()).isEqualTo(1);
        assertThat(golfinhos.adminEmails()).containsExactly("admin@golfinhos.com");

        SchoolSummary aquaflow = schools.get(0);
        assertThat(aquaflow.adminEmails()).contains("admin@aquaflow.com").doesNotContain("admin@golfinhos.com");
        assertThat(aquaflow.pools()).isGreaterThan(golfinhos.pools());
    }

    @Test
    void soAEquipeAquaflowAbreATela() throws Exception {
        mvc.perform(get("/plataforma/escolas").session(login("admin@golfinhos.com", "admin123")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/plataforma/escolas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user(email).password(password))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();
    }
}

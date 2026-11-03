package br.edu.aquaflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicacao inteira contra um Postgres real (Testcontainers), rodando as migrations do Flyway.
 * Se o schema, uma migration ou um template publico quebrar, o CI falha antes do deploy.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PublicPagesSmokeTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Test
    void homeMostraNomeDaEscolaComAcentuacaoCorreta() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AquaFlow Escola de Natação")));
    }

    @Test
    void planosSaoPublicosEListamOsPlanosDoBanco() throws Exception {
        mvc.perform(get("/planos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Essencial")))
                .andExpect(content().string(containsString("Regular")))
                .andExpect(content().string(containsString("Intensivo")));
    }

    @Test
    void paginasDeAutenticacaoAbrem() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void areaAdministrativaExigeLogin() throws Exception {
        mvc.perform(get("/admin/pools"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void loginDeDemonstracaoFunciona() throws Exception {
        mvc.perform(formLogin("/login").user("admin@aquaflow.com").password("admin123"))
                .andExpect(authenticated());
    }
}

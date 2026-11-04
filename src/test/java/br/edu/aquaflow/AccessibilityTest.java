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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/** Itens de acessibilidade comuns a todas as páginas públicas. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AccessibilityTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Test
    void paginasPublicasTemAtalhoParaOConteudoETelefoneClicavel() throws Exception {
        for (String page : new String[]{"/", "/planos", "/loja"}) {
            mvc.perform(get(page))
                    .andExpect(content().string(containsString("href=\"#conteudo\"")))
                    .andExpect(content().string(containsString("id=\"conteudo\"")))
                    .andExpect(content().string(containsString("href=\"tel:")));
        }
    }
}

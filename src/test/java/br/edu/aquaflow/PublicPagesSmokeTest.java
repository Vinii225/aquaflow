package br.edu.aquaflow;

import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.service.AuthService;
import br.edu.aquaflow.service.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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

    @Autowired
    AuthService authService;

    @Autowired
    PlanRepository planRepository;

    @Autowired
    TenantContext tenantContext;

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
    void homeAgrupaOsDiasDaMesmaTurmaEMostraOPrecoInicial() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Terça e Quinta")))
                .andExpect(content().string(containsString("99,90")));
    }

    @Test
    void lojaEPublicaEListaProdutosComDescontoDosPlanos() throws Exception {
        mvc.perform(get("/loja"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Óculos de natação")))
                .andExpect(content().string(containsString("Nadadeiras de treino")))
                .andExpect(content().string(containsString("Regular 10%")));
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

    @Test
    void contratarPlanoExigeLoginEVoltaParaOPagamentoDepois() throws Exception {
        String checkout = "/planos/" + planId("Regular") + "/contratar";
        authService.registerStudent("Aluna Teste", "contrato-login@teste.com", "senha123");

        MockHttpSession session = (MockHttpSession) mvc.perform(get(checkout))
                .andExpect(redirectedUrlPattern("**/login"))
                .andReturn().getRequest().getSession();

        mvc.perform(get("/login").session(session))
                .andExpect(content().string(containsString("concluir a contratação")));

        mvc.perform(post("/login").session(session).with(csrf())
                        .param("username", "contrato-login@teste.com").param("password", "senha123"))
                .andExpect(redirectedUrlPattern("**" + checkout + "*"));
    }

    @Test
    void alunoContrataPlanoEVeAConfirmacao() throws Exception {
        authService.registerStudent("Aluno Teste", "contrato@teste.com", "senha123");
        MockHttpSession session = login("contrato@teste.com", "senha123");
        String checkout = "/planos/" + planId("Intensivo") + "/contratar";

        mvc.perform(get(checkout).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Contratar o plano")))
                .andExpect(content().string(containsString("199,90")));

        mvc.perform(post(checkout).session(session).with(csrf())
                        .param("dueDay", "10").param("paymentMethod", "PIX"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirme que leu as condições")));

        String confirmation = mvc.perform(post(checkout).session(session).with(csrf())
                        .param("dueDay", "10").param("paymentMethod", "PIX").param("acceptTerms", "true"))
                .andExpect(redirectedUrlPattern("/planos/contratacao/*"))
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(confirmation).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bem-vindo ao plano")))
                .andExpect(content().string(containsString("Paga")));

        mvc.perform(get("/planos").session(session))
                .andExpect(content().string(containsString("Seu plano atual")));

        mvc.perform(get(checkout).session(session))
                .andExpect(redirectedUrl("/planos"));
    }

    @Test
    void confirmacaoDeOutroAlunoNaoAparece() throws Exception {
        authService.registerStudent("Dono", "dono@teste.com", "senha123");
        authService.registerStudent("Curioso", "curioso@teste.com", "senha123");
        MockHttpSession dono = login("dono@teste.com", "senha123");
        String confirmation = mvc.perform(post("/planos/" + planId("Essencial") + "/contratar").session(dono).with(csrf())
                        .param("dueDay", "5").param("paymentMethod", "BOLETO").param("acceptTerms", "true"))
                .andReturn().getResponse().getRedirectedUrl();

        mvc.perform(get(confirmation).session(login("curioso@teste.com", "senha123")))
                .andExpect(status().isNotFound());
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user(email).password(password))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();
    }

    private String planId(String name) {
        return planRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantContext.getTenantId()).stream()
                .filter(p -> p.getName().equals(name))
                .findFirst().orElseThrow().getId().toString();
    }
}

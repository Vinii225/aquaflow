package br.edu.aquaflow;

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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Busca e filtro na loja (PB-03 / S4-04), com os produtos de demonstração das duas escolas. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class StoreSearchTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Test
    void buscaPorNomeSemAcentoNemMaiuscula() throws Exception {
        mvc.perform(get("/loja").param("q", "OCULOS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Óculos de natação")))
                .andExpect(content().string(not(containsString("Prancha de EVA"))))
                .andExpect(content().string(containsString("1 produto encontrado")));
    }

    @Test
    void filtroPorCategoria() throws Exception {
        mvc.perform(get("/loja").param("categoria", "treino"))
                .andExpect(content().string(allOf(
                        containsString("Prancha de EVA"), containsString("Pull buoy"), containsString("Nadadeiras de treino"))))
                .andExpect(content().string(not(containsString("Touca de silicone"))));
    }

    @Test
    void filtroPorFaixaDePreco() throws Exception {
        mvc.perform(get("/loja").param("min", "60").param("max", "80"))
                .andExpect(content().string(allOf(containsString("Prancha de EVA"), containsString("Pull buoy"))))
                .andExpect(content().string(not(containsString("Óculos de natação"))))
                .andExpect(content().string(not(containsString("Nadadeiras de treino"))));
    }

    @Test
    void buscaECategoriaEPrecoCombinados() throws Exception {
        // "de" casa com Prancha, Nadadeiras, Óculos, Touca e Toalha; treino tira os acessórios; max tira as Nadadeiras
        mvc.perform(get("/loja").param("q", "de").param("categoria", "treino").param("max", "100"))
                .andExpect(content().string(containsString("Prancha de EVA")))
                .andExpect(content().string(containsString("1 produto encontrado")))
                .andExpect(content().string(not(containsString("Nadadeiras de treino"))))
                .andExpect(content().string(not(containsString("Óculos de natação"))))
                .andExpect(content().string(not(containsString("Pull buoy"))));
    }

    @Test
    void formularioReabreComOsValoresDaUrl() throws Exception {
        mvc.perform(get("/loja?q=oculos&categoria=acessorios&min=10&max=99.9"))
                .andExpect(content().string(containsString("value=\"oculos\"")))
                .andExpect(content().string(containsString("value=\"acessorios\" selected")))
                .andExpect(content().string(containsString("value=\"10\"")))
                .andExpect(content().string(containsString("value=\"99.9\"")));
    }

    @Test
    void semResultadoMostraMensagemELinkParaLimpar() throws Exception {
        mvc.perform(get("/loja").param("q", "touca").param("categoria", "treino"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum produto encontrado com esses filtros")))
                .andExpect(content().string(containsString("Limpar filtros e ver todos")));
    }

    @Test
    void parametrosInvalidosNaoQuebramAPagina() throws Exception {
        mvc.perform(get("/loja").param("categoria", "xyz").param("min", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Óculos de natação")));
    }

    @Test
    void buscaMostraSoProdutosDaEscolaAtual() throws Exception {
        mvc.perform(get("/loja").param("q", "oculos"))
                .andExpect(content().string(containsString("Óculos de natação")))
                .andExpect(content().string(not(containsString("Óculos espelhado Golfinhos"))));

        MockHttpSession alunoB = (MockHttpSession) mvc.perform(formLogin("/login").user("aluno@golfinhos.com").password("aluno123"))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();
        mvc.perform(get("/loja").param("q", "oculos").session(alunoB))
                .andExpect(content().string(containsString("Óculos espelhado Golfinhos")))
                .andExpect(content().string(not(containsString("Óculos de natação"))));
    }

    @Test
    void adicionarAoCarrinhoVoltaComOsMesmosFiltros() throws Exception {
        mvc.perform(post("/loja/carrinho").with(csrf())
                        .param("productId", "88888888-8888-8888-8888-888888888804")
                        .param("q", "prancha eva").param("categoria", "treino"))
                .andExpect(redirectedUrl("/loja?q=prancha+eva&categoria=treino#produto-88888888-8888-8888-8888-888888888804"));
    }
}

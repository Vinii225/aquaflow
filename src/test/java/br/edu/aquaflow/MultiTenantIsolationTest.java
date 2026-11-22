package br.edu.aquaflow;

import br.edu.aquaflow.domain.ContactMessage;
import br.edu.aquaflow.domain.StoreOrder;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.OrderStatus;
import br.edu.aquaflow.repository.ContactMessageRepository;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.StoreOrderRepository;
import br.edu.aquaflow.repository.TenantRepository;
import br.edu.aquaflow.repository.UserRepository;
import br.edu.aquaflow.service.BillingService;
import br.edu.aquaflow.service.ContactMessageService;
import br.edu.aquaflow.service.TenantContext;
import br.edu.aquaflow.web.dto.ContactForm;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Isolamento por {@code tenant_id} (PB-09): com as duas escolas de demonstração (V2 e V8),
 * a escola A não vê piscinas, faturas, pedidos nem mensagens da escola B, e vice-versa.
 * Critério obrigatório do Plano de Qualidade da Sprint #4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MultiTenantIsolationTest {

    static final UUID ESCOLA_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID ESCOLA_B = UUID.fromString("11111111-1111-1111-1111-111111111112");
    static final String PISCINA_B = "44444444-4444-4444-4444-4444444444b1";
    static final UUID FATURA_B = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaab1");
    static final UUID ALUNO_A = UUID.fromString("22222222-2222-2222-2222-222222222204");
    static final UUID ALUNO_B = UUID.fromString("22222222-2222-2222-2222-2222222222b3");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired TenantContext tenantContext;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired BillingService billingService;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired StoreOrderRepository storeOrderRepository;
    @Autowired ContactMessageService contactMessageService;
    @Autowired ContactMessageRepository contactMessageRepository;

    @Test
    void visitanteAnonimoVeAEscolaPadrao() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AquaFlow Escola de Natação")))
                .andExpect(content().string(containsString("Av. das Piscinas, 123 - Centro")))
                .andExpect(content().string(not(containsString("Golfinhos Natação"))));
    }

    @Test
    void usuarioLogadoVeOsDadosDaPropriaEscola() throws Exception {
        MockHttpSession alunoB = login("aluno@golfinhos.com", "aluno123");

        mvc.perform(get("/loja").session(alunoB))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Golfinhos Natação")))
                .andExpect(content().string(containsString("Rua das Ondas, 456")))
                .andExpect(content().string(containsString("Palmar de treino")))
                .andExpect(content().string(not(containsString("Nadadeiras de treino"))));

        mvc.perform(get("/planos").session(alunoB))
                .andExpect(content().string(containsString("Golfinho Livre")))
                .andExpect(content().string(not(containsString("Intensivo"))));
    }

    @Test
    void piscinas_cadaAdminSoVeAsDaPropriaEscola() throws Exception {
        mvc.perform(get("/admin/pools").session(login("admin@aquaflow.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Piscina Semiolímpica")))
                .andExpect(content().string(not(containsString("Piscina Coberta Golfinhos"))));

        mvc.perform(get("/admin/pools").session(login("admin@golfinhos.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Piscina Coberta Golfinhos")))
                .andExpect(content().string(not(containsString("Piscina Semiolímpica"))));
    }

    @Test
    void piscinas_idDeOutraEscolaResponde404() throws Exception {
        MockHttpSession adminA = login("admin@aquaflow.com", "admin123");

        mvc.perform(get("/admin/pools/" + PISCINA_B + "/edit").session(adminA))
                .andExpect(status().isNotFound());
        mvc.perform(post("/admin/pools/" + PISCINA_B + "/delete").session(adminA).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void turmas_formularioRecusaPiscinaDeOutraEscola() throws Exception {
        mvc.perform(post("/admin/classes").session(login("admin@aquaflow.com", "admin123")).with(csrf())
                        .param("name", "Turma invasora").param("pool", PISCINA_B)
                        .param("maxCapacity", "5").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escolha uma piscina da escola")));
    }

    @Test
    void faturas_cadaEscolaSoVeAsSuas() throws Exception {
        tenantContext.runAs(ESCOLA_A, () -> billingService.generateInvoicesForMonth(YearMonth.now()));

        mvc.perform(get("/admin/invoices").session(login("admin@aquaflow.com", "admin123")))
                .andExpect(content().string(containsString("Aluno Demonstração")))
                .andExpect(content().string(not(containsString("Aluno Golfinhos"))));

        mvc.perform(get("/admin/invoices").session(login("admin@golfinhos.com", "admin123")))
                .andExpect(content().string(containsString("Aluno Golfinhos")))
                .andExpect(content().string(not(containsString("Aluno Demonstração"))));

        assertThat(tenantContext.callAs(ESCOLA_B, () -> invoiceRepository.findByTenantIdAndStudentId(ESCOLA_B, ALUNO_A)))
                .isEmpty();
    }

    @Test
    void faturas_escolaANaoRegistraPagamentoDeFaturaDaB() {
        UUID adminA = UUID.fromString("22222222-2222-2222-2222-222222222201");

        assertThatThrownBy(() -> tenantContext.runAs(ESCOLA_A,
                () -> billingService.registerPayment(FATURA_B, "pix", adminA)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(invoiceRepository.findById(FATURA_B).orElseThrow().getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }

    @Test
    void pedidos_cadaEscolaSoVeOsSeus() throws Exception {
        StoreOrder pedidoA = order(ESCOLA_A, ALUNO_A);
        StoreOrder pedidoB = order(ESCOLA_B, ALUNO_B);

        mvc.perform(get("/admin").session(login("admin@aquaflow.com", "admin123")))
                .andExpect(content().string(containsString(pedidoA.getCode())))
                .andExpect(content().string(not(containsString(pedidoB.getCode()))));

        mvc.perform(get("/admin").session(login("admin@golfinhos.com", "admin123")))
                .andExpect(content().string(containsString(pedidoB.getCode())))
                .andExpect(content().string(not(containsString(pedidoA.getCode()))));

        MockHttpSession alunoB = login("aluno@golfinhos.com", "aluno123");
        mvc.perform(get("/student/orders").session(alunoB))
                .andExpect(content().string(containsString(pedidoB.getCode())))
                .andExpect(content().string(not(containsString(pedidoA.getCode()))));
        mvc.perform(get("/loja/pedidos/" + pedidoA.getId()).session(alunoB))
                .andExpect(status().isNotFound());
    }

    @Test
    void mensagens_ficamNaEscolaDeOrigem() throws Exception {
        mvc.perform(post("/planos/contato").with(csrf())
                        .param("name", "Interessado A").param("email", "a@teste.com")
                        .param("message", "Quero saber dos horarios"))
                .andExpect(status().is3xxRedirection());
        tenantContext.runAs(ESCOLA_B, () -> contactMessageService.send(contact("Interessado B"), null));

        ContactMessage deA = message("Interessado A");
        ContactMessage deB = message("Interessado B");
        assertThat(deA.getTenant().getId()).isEqualTo(ESCOLA_A);
        assertThat(deB.getTenant().getId()).isEqualTo(ESCOLA_B);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user(email).password(password))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();
    }

    private StoreOrder order(UUID tenantId, UUID studentId) {
        StoreOrder order = new StoreOrder();
        order.setTenant(tenantRepository.findById(tenantId).orElseThrow());
        order.setStudent(userRepository.findById(studentId).orElseThrow());
        order.setStatus(OrderStatus.PAID);
        order.setSubtotal(new BigDecimal("10.00"));
        order.setTotal(new BigDecimal("10.00"));
        order.setPaymentMethod("pix");
        order.setPaidAt(Instant.now());
        return storeOrderRepository.save(order);
    }

    private static ContactForm contact(String name) {
        ContactForm form = new ContactForm();
        form.setName(name);
        form.setEmail("b@teste.com");
        form.setMessage("Quero saber dos horarios");
        return form;
    }

    private ContactMessage message(String name) {
        return contactMessageRepository.findAll().stream()
                .filter(m -> m.getName().equals(name))
                .findFirst().orElseThrow();
    }
}

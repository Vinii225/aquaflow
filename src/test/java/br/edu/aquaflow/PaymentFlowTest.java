package br.edu.aquaflow;

import br.edu.aquaflow.domain.GatewayPayment;
import br.edu.aquaflow.domain.Invoice;
import br.edu.aquaflow.domain.StoreOrder;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.OrderStatus;
import br.edu.aquaflow.domain.enums.PaymentStatus;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.repository.GatewayPaymentRepository;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.StoreOrderRepository;
import br.edu.aquaflow.repository.SubscriptionRepository;
import br.edu.aquaflow.repository.TransactionRepository;
import br.edu.aquaflow.service.AuthService;
import br.edu.aquaflow.service.payment.GatewayPagamento;
import br.edu.aquaflow.service.payment.PagamentoSimuladoGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Pagamento online (S4-06) com o gateway simulado: aprovado, pendente e recusado na contratação
 * de plano (fatura) e no checkout da loja (pedido), atualização pelo retorno (back_url) e pelo
 * webhook, idempotência e escola certa. Nenhum teste chama a API do Mercado Pago.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentFlowTest {

    static final String PLANO_REGULAR = "77777777-7777-7777-7777-777777777702";
    static final String PLANO_GOLFINHO_BASICO = "77777777-7777-7777-7777-7777777777b1";
    static final String OCULOS = "88888888-8888-8888-8888-888888888801";
    static final UUID ESCOLA_B = UUID.fromString("11111111-1111-1111-1111-111111111112");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired AuthService authService;
    @Autowired GatewayPagamento gateway;
    @Autowired GatewayPaymentRepository paymentRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired StoreOrderRepository orderRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired TransactionTemplate tx;

    @AfterEach
    void voltaAoPadrao() {
        simulado().setNextOutcome(PaymentStatus.APPROVED);
    }

    @Test
    void semChaveDoMercadoPagoUsaOPagamentoSimulado() {
        assertThat(gateway).isInstanceOf(PagamentoSimuladoGateway.class);
    }

    // ---------- Contratação de plano (fatura) ----------

    @Test
    void plano_aprovado_faturaPagaComEntradaNoCaixa() throws Exception {
        MockHttpSession aluno = novoAluno("plano-aprovado@teste.com");
        long caixaAntes = transactionRepository.count();

        String confirmacao = contratar(aluno, PLANO_REGULAR);

        mvc.perform(get(confirmacao).session(aluno))
                .andExpect(content().string(containsString("Bem-vindo ao plano")))
                .andExpect(content().string(containsString("Paga")));
        GatewayPayment pagamento = pagamentoDaAssinatura(confirmacao);
        assertThat(pagamento.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(faturaDe(pagamento).getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes + 1);
    }

    @Test
    void plano_pendente_faturaEmAbertoEWebhookConfirmaUmaVezSo() throws Exception {
        simulado().setNextOutcome(PaymentStatus.PENDING);
        MockHttpSession aluno = novoAluno("plano-pendente@teste.com");
        long caixaAntes = transactionRepository.count();

        String confirmacao = contratar(aluno, PLANO_REGULAR);

        mvc.perform(get(confirmacao).session(aluno))
                .andExpect(content().string(containsString("Pagamento em análise")))
                .andExpect(content().string(containsString("Em aberto")));
        GatewayPayment pagamento = pagamentoDaAssinatura(confirmacao);
        assertThat(faturaDe(pagamento).getStatus()).isEqualTo(InvoiceStatus.OPEN);

        // O Pix foi pago: o "gateway" muda o status e notifica. Notificação repetida não lança o caixa de novo.
        simulado().changeStatus(pagamento.getGatewayPaymentId(), PaymentStatus.APPROVED);
        webhook(pagamento.getGatewayPaymentId());
        webhook(pagamento.getGatewayPaymentId());

        assertThat(paymentRepository.findById(pagamento.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(faturaDe(pagamento).getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes + 1);
    }

    @Test
    void plano_recusado_cancelaFaturaEAssinaturaEPermiteTentarDeNovo() throws Exception {
        simulado().setNextOutcome(PaymentStatus.REJECTED);
        MockHttpSession aluno = novoAluno("plano-recusado@teste.com");
        long caixaAntes = transactionRepository.count();

        String confirmacao = contratar(aluno, PLANO_REGULAR);

        mvc.perform(get(confirmacao).session(aluno))
                .andExpect(content().string(containsString("Pagamento recusado")))
                .andExpect(content().string(containsString("Tentar de novo")));
        GatewayPayment pagamento = pagamentoDaAssinatura(confirmacao);
        assertThat(faturaDe(pagamento).getStatus()).isEqualTo(InvoiceStatus.CANCELED);
        assertThat(subscriptionRepository.findById(assinatura(confirmacao)).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes);

        mvc.perform(get("/planos/" + PLANO_REGULAR + "/contratar").session(aluno))
                .andExpect(status().isOk());
    }

    @Test
    void plano_pagamentoFicaNaEscolaDoAluno() throws Exception {
        MockHttpSession alunoB = login("aluno@golfinhos.com", "aluno123");
        // O aluno de demonstração da escola B já tem plano: cancela para poder contratar
        tx.executeWithoutResult(s -> subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getTenant().getId().equals(ESCOLA_B) && sub.getStatus() == SubscriptionStatus.ACTIVE)
                .forEach(sub -> sub.setStatus(SubscriptionStatus.CANCELED)));

        String confirmacao = contratar(alunoB, PLANO_GOLFINHO_BASICO);

        assertThat(pagamentoDaAssinatura(confirmacao).getTenant().getId()).isEqualTo(ESCOLA_B);
    }

    // ---------- Checkout da loja (pedido) ----------

    @Test
    void loja_aprovado_pedidoPagoComEntradaNoCaixa() throws Exception {
        MockHttpSession aluno = novoAluno("loja-aprovado@teste.com");
        long caixaAntes = transactionRepository.count();

        String pedido = comprar(aluno);

        mvc.perform(get(pedido).session(aluno)).andExpect(content().string(containsString("Pedido confirmado")));
        assertThat(pedido(pedido).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes + 1);
    }

    @Test
    void loja_pendente_retornoDoAlunoConsultaOGatewayEConfirma() throws Exception {
        simulado().setNextOutcome(PaymentStatus.PENDING);
        MockHttpSession aluno = novoAluno("loja-pendente@teste.com");
        long caixaAntes = transactionRepository.count();

        String pedido = comprar(aluno);

        mvc.perform(get(pedido).session(aluno)).andExpect(content().string(containsString("Pagamento em análise")));
        assertThat(pedido(pedido).getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes);

        // Volta pela back_url de "pendente", mas a API já diz aprovado: vale a API, não a URL
        GatewayPayment pagamento = pagamentoDoPedido(pedido);
        simulado().changeStatus(pagamento.getGatewayPaymentId(), PaymentStatus.APPROVED);
        mvc.perform(get("/pagamentos/retorno/pendente").session(aluno)
                        .param("payment_id", pagamento.getGatewayPaymentId())
                        .param("external_reference", pagamento.getId().toString())
                        .param("status", "pending"))
                .andExpect(redirectedUrl(pedido));

        assertThat(pedido(pedido).getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes + 1);
    }

    @Test
    void loja_recusado_pedidoRecusadoEItensVoltamParaOCarrinho() throws Exception {
        simulado().setNextOutcome(PaymentStatus.REJECTED);
        MockHttpSession aluno = novoAluno("loja-recusado@teste.com");
        long caixaAntes = transactionRepository.count();

        String pedido = comprar(aluno);

        mvc.perform(get(pedido).session(aluno)).andExpect(content().string(containsString("Pagamento recusado")));
        assertThat(pedido(pedido).getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(transactionRepository.count()).isEqualTo(caixaAntes);
        mvc.perform(get("/loja/carrinho").session(aluno)).andExpect(content().string(containsString("Óculos de natação")));
    }

    // ---------- Retorno e webhook: segurança ----------

    @Test
    void retornoDePagamentoDeOutroAlunoResponde404() throws Exception {
        String pedido = comprar(novoAluno("dono-pedido@teste.com"));
        GatewayPayment pagamento = pagamentoDoPedido(pedido);

        mvc.perform(get("/pagamentos/retorno/sucesso").session(novoAluno("curioso-pedido@teste.com"))
                        .param("payment_id", pagamento.getGatewayPaymentId())
                        .param("external_reference", pagamento.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void webhookIgnoraPagamentoDesconhecidoEOutrosTipos() throws Exception {
        mvc.perform(post("/pagamentos/webhook/mercadopago").param("data.id", "123").param("type", "payment"))
                .andExpect(status().isOk());
        mvc.perform(post("/pagamentos/webhook/mercadopago").param("data.id", "123").param("type", "merchant_order"))
                .andExpect(status().isOk());
    }

    @Test
    void webhookAceitaODataIdNoCorpoJson() throws Exception {
        simulado().setNextOutcome(PaymentStatus.PENDING);
        String pedido = comprar(novoAluno("loja-webhook-json@teste.com"));
        GatewayPayment pagamento = pagamentoDoPedido(pedido);
        simulado().changeStatus(pagamento.getGatewayPaymentId(), PaymentStatus.APPROVED);

        mvc.perform(post("/pagamentos/webhook/mercadopago").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment\",\"action\":\"payment.updated\",\"data\":{\"id\":\""
                                + pagamento.getGatewayPaymentId() + "\"}}"))
                .andExpect(status().isOk());

        assertThat(pedido(pedido).getStatus()).isEqualTo(OrderStatus.PAID);
    }

    // ---------- Apoio ----------

    private PagamentoSimuladoGateway simulado() {
        return (PagamentoSimuladoGateway) gateway;
    }

    private MockHttpSession novoAluno(String email) throws Exception {
        authService.registerStudent("Aluno " + email, email, "senha123");
        return login(email, "senha123");
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user(email).password(password))
                .andExpect(authenticated())
                .andReturn().getRequest().getSession();
    }

    /** Contrata o plano e devolve a URL da confirmação (/planos/contratacao/{assinatura}). */
    private String contratar(MockHttpSession aluno, String planId) throws Exception {
        return mvc.perform(post("/planos/" + planId + "/contratar").session(aluno).with(csrf())
                        .param("dueDay", "10").param("paymentMethod", "PIX").param("acceptTerms", "true"))
                .andExpect(redirectedUrlPattern("/planos/contratacao/*"))
                .andReturn().getResponse().getRedirectedUrl();
    }

    /** Põe os óculos no carrinho, finaliza e devolve a URL do pedido (/loja/pedidos/{id}). */
    private String comprar(MockHttpSession aluno) throws Exception {
        mvc.perform(post("/loja/carrinho").session(aluno).with(csrf()).param("productId", OCULOS));
        return mvc.perform(post("/loja/checkout").session(aluno).with(csrf()).param("paymentMethod", "PIX"))
                .andExpect(redirectedUrlPattern("/loja/pedidos/*"))
                .andReturn().getResponse().getRedirectedUrl();
    }

    private void webhook(String gatewayPaymentId) throws Exception {
        mvc.perform(post("/pagamentos/webhook/mercadopago").param("data.id", gatewayPaymentId).param("type", "payment"))
                .andExpect(status().isOk());
    }

    private static UUID idNoFim(String url) {
        return UUID.fromString(url.substring(url.lastIndexOf('/') + 1));
    }

    private UUID assinatura(String confirmacao) {
        return idNoFim(confirmacao);
    }

    private GatewayPayment pagamentoDaAssinatura(String confirmacao) {
        UUID subscriptionId = assinatura(confirmacao);
        return tx.execute(s -> {
            Invoice invoice = invoiceRepository.findBySubscriptionIdOrderByReferenceMonthDesc(subscriptionId).get(0);
            GatewayPayment p = pagamentoDe(invoice.getId());
            p.getTenant().getId();
            return p;
        });
    }

    private GatewayPayment pagamentoDoPedido(String pedido) {
        return pagamentoDe(idNoFim(pedido));
    }

    private GatewayPayment pagamentoDe(UUID referenceId) {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getReferenceId().equals(referenceId))
                .findFirst().orElseThrow();
    }

    private Invoice faturaDe(GatewayPayment pagamento) {
        return invoiceRepository.findById(pagamento.getReferenceId()).orElseThrow();
    }

    private StoreOrder pedido(String url) {
        return orderRepository.findById(idNoFim(url)).orElseThrow();
    }
}

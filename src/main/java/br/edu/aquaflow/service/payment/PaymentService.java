package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.domain.GatewayPayment;
import br.edu.aquaflow.domain.Invoice;
import br.edu.aquaflow.domain.StoreOrder;
import br.edu.aquaflow.domain.Subscription;
import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.OrderStatus;
import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.domain.enums.PaymentReference;
import br.edu.aquaflow.domain.enums.PaymentStatus;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.repository.GatewayPaymentRepository;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.StoreOrderRepository;
import br.edu.aquaflow.repository.SubscriptionRepository;
import br.edu.aquaflow.service.BillingService;
import br.edu.aquaflow.service.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Pagamento online das faturas (PB-02) e dos pedidos da loja (PB-04), pelo {@link GatewayPagamento}.
 * <ul>
 *   <li>aprovado: fatura paga / pedido pago, com entrada no caixa;</li>
 *   <li>pendente: fatura em aberto / pedido aguardando pagamento;</li>
 *   <li>recusado: fatura e assinatura canceladas / pedido recusado.</li>
 * </ul>
 * O status é atualizado no retorno do aluno (back_urls) e pelo webhook, sempre consultando o
 * gateway. {@link #apply} é idempotente: notificações repetidas não lançam o caixa duas vezes.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM/yyyy");

    /** Resultado do início do pagamento: redirecionar para o gateway ou mostrar o status. */
    public record Start(UUID paymentId, PaymentStatus status, String redirectUrl) {

        public boolean isRedirect() {
            return redirectUrl != null;
        }
    }

    /** Onde o pagamento está ligado, para a tela de retorno. */
    public record Result(PaymentReference type, UUID referenceId, UUID subscriptionId, PaymentStatus status) {
    }

    private final GatewayPagamento gateway;
    private final GatewayPaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StoreOrderRepository orderRepository;
    private final BillingService billingService;
    private final TenantContext tenantContext;

    public PaymentService(GatewayPagamento gateway,
                          GatewayPaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          SubscriptionRepository subscriptionRepository,
                          StoreOrderRepository orderRepository,
                          BillingService billingService,
                          TenantContext tenantContext) {
        this.gateway = gateway;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
        this.billingService = billingService;
        this.tenantContext = tenantContext;
    }

    public boolean isExternalGateway() {
        return gateway.isExternal();
    }

    @Transactional
    public Start startInvoice(Invoice invoice, PaymentMethod method, User payer, String baseUrl) {
        String title = "Mensalidade " + invoice.getSubscription().getPlanName()
                + " - " + MONTH.format(invoice.getReferenceMonth());
        return start(PaymentReference.INVOICE, invoice.getId(), invoice.getTenant(), invoice.getAmount(),
                method, payer, title, baseUrl);
    }

    @Transactional
    public Start startOrder(StoreOrder order, PaymentMethod method, User payer, String baseUrl) {
        return start(PaymentReference.STORE_ORDER, order.getId(), order.getTenant(), order.getTotal(),
                method, payer, "Pedido " + order.getCode() + " - loja AquaFlow", baseUrl);
    }

    /** Consulta o pagamento no gateway e atualiza a fatura/pedido. Usado pelo webhook. */
    @Transactional
    public Optional<GatewayPayment> refresh(String gatewayPaymentId) {
        Optional<GatewayPagamento.GatewayStatus> found = gateway.lookup(gatewayPaymentId);
        if (found.isEmpty()) {
            log.info("Pagamento {} não encontrado no gateway {}", gatewayPaymentId, gateway.name());
            return Optional.empty();
        }
        GatewayPagamento.GatewayStatus status = found.get();
        Optional<GatewayPayment> payment = parseUuid(status.externalReference()).flatMap(paymentRepository::lockById);
        if (payment.isEmpty()) {
            log.warn("Pagamento {} com external_reference desconhecida: {}", gatewayPaymentId, status.externalReference());
            return Optional.empty();
        }
        apply(payment.get(), status);
        return payment;
    }

    /**
     * Volta do aluno pelas back_urls. Não confia no status da URL: consulta o gateway pelo
     * {@code payment_id}. Só acha pagamentos do próprio aluno, na escola da sessão.
     */
    @Transactional
    public Optional<Result> handleReturn(UUID paymentId, String gatewayPaymentId, UUID payerId) {
        Optional<GatewayPayment> own = paymentRepository.findByIdAndTenantId(paymentId, tenantContext.getTenantId())
                .filter(p -> p.getPayer().getId().equals(payerId));
        if (own.isEmpty()) {
            return Optional.empty();
        }
        GatewayPayment payment = paymentRepository.lockById(paymentId).orElseThrow();
        if (gatewayPaymentId != null) {
            gateway.lookup(gatewayPaymentId)
                    .filter(s -> paymentId.toString().equals(s.externalReference()))
                    .ifPresent(s -> apply(payment, s));
        }
        return Optional.of(result(payment));
    }

    /**
     * Aplica o status do gateway. Aprovado é final. Só a mudança de status mexe na fatura/pedido,
     * então repetir a mesma notificação não tem efeito.
     */
    void apply(GatewayPayment payment, GatewayPagamento.GatewayStatus status) {
        if (status.gatewayPaymentId() != null) {
            payment.setGatewayPaymentId(status.gatewayPaymentId());
        }
        PaymentStatus next = status.status();
        if (payment.getStatus() == PaymentStatus.APPROVED || payment.getStatus() == next) {
            paymentRepository.save(payment);
            return;
        }
        if (next == PaymentStatus.APPROVED && status.amount() != null
                && status.amount().compareTo(payment.getAmount()) != 0) {
            log.warn("Pagamento {} aprovado com valor {} diferente do esperado {}: ignorado",
                    payment.getId(), status.amount(), payment.getAmount());
            return;
        }

        payment.setStatus(next);
        payment.setStatusUpdatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);

        tenantContext.runAs(payment.getTenant().getId(), () -> {
            if (payment.getReferenceType() == PaymentReference.INVOICE) {
                updateInvoice(payment, next);
            } else {
                updateOrder(payment, next);
            }
        });
    }

    private Start start(PaymentReference type, UUID referenceId, Tenant tenant,
                        BigDecimal amount, PaymentMethod method, User payer, String title, String baseUrl) {
        GatewayPayment payment = new GatewayPayment();
        payment.setTenant(tenant);
        payment.setReferenceType(type);
        payment.setReferenceId(referenceId);
        payment.setGateway(gateway.name());
        payment.setAmount(amount);
        payment.setPaymentMethod(method.getCode());
        payment.setPayer(payer);
        paymentRepository.save(payment);

        String base = stripTrailingSlash(baseUrl);
        String notification = base.startsWith("https://") ? base + "/pagamentos/webhook/mercadopago" : null;
        GatewayPagamento.Checkout checkout = gateway.start(new GatewayPagamento.CheckoutRequest(
                payment.getId().toString(), title, amount, method,
                base + "/pagamentos/retorno/sucesso",
                base + "/pagamentos/retorno/pendente",
                base + "/pagamentos/retorno/falha",
                notification));

        payment.setGatewayPreferenceId(checkout.preferenceId());
        if (checkout.immediate() != null) {
            apply(payment, checkout.immediate());
        } else {
            paymentRepository.save(payment);
        }
        return new Start(payment.getId(), payment.getStatus(), checkout.redirectUrl());
    }

    private void updateInvoice(GatewayPayment payment, PaymentStatus status) {
        Invoice invoice = invoiceRepository.findByIdAndTenantId(payment.getReferenceId(), payment.getTenant().getId())
                .orElseThrow(() -> new IllegalStateException("Fatura do pagamento não encontrada: " + payment.getId()));
        Subscription subscription = invoice.getSubscription();
        switch (status) {
            case APPROVED -> {
                if (invoice.getStatus() != InvoiceStatus.PAID) {
                    billingService.registerPayment(invoice.getId(), payment.getPaymentMethod(), payment.getPayer().getId());
                }
                // Aprovado depois de uma tentativa recusada: a contratação volta a valer
                if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setUpdatedAt(Instant.now());
                }
            }
            case REJECTED -> {
                if (invoice.getStatus() == InvoiceStatus.OPEN) {
                    invoice.setStatus(InvoiceStatus.CANCELED);
                    invoice.setUpdatedAt(Instant.now());
                    subscription.setStatus(SubscriptionStatus.CANCELED);
                    subscription.setUpdatedAt(Instant.now());
                }
            }
            case PENDING -> { /* fatura continua em aberto */ }
        }
    }

    private void updateOrder(GatewayPayment payment, PaymentStatus status) {
        StoreOrder order = orderRepository.findByIdAndTenantId(payment.getReferenceId(), payment.getTenant().getId())
                .orElseThrow(() -> new IllegalStateException("Pedido do pagamento não encontrado: " + payment.getId()));
        switch (status) {
            case APPROVED -> {
                if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DELIVERED) {
                    order.setStatus(OrderStatus.PAID);
                    order.setPaidAt(Instant.now());
                    if (order.getTotal().signum() > 0) {
                        billingService.registerIncome(order.getTenant(), "store", "store_order", order.getId(),
                                order.getTotal(), payment.getPaymentMethod(), payment.getPayer().getId());
                    }
                }
            }
            case REJECTED -> {
                if (order.getStatus() == OrderStatus.PENDING) {
                    order.setStatus(OrderStatus.REJECTED);
                }
            }
            case PENDING -> order.setStatus(OrderStatus.PENDING);
        }
    }

    private Result result(GatewayPayment payment) {
        UUID subscriptionId = payment.getReferenceType() == PaymentReference.INVOICE
                ? invoiceRepository.findByIdAndTenantId(payment.getReferenceId(), payment.getTenant().getId())
                        .map(i -> i.getSubscription().getId()).orElse(null)
                : null;
        return new Result(payment.getReferenceType(), payment.getReferenceId(), subscriptionId, payment.getStatus());
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

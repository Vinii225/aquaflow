package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Meio de pagamento online usado na contratação de plano (PB-02) e no checkout da loja (PB-04).
 * Implementações: {@link MercadoPagoGateway} (quando {@code MERCADOPAGO_ACCESS_TOKEN} está definida)
 * e {@link PagamentoSimuladoGateway} (padrão, para CI e homologação sem chave).
 * A escolha fica em {@link PaymentGatewayConfig}.
 */
public interface GatewayPagamento {

    /** Gravado em {@code gateway_payments.gateway}: {@code SIMULATED} ou {@code MERCADO_PAGO}. */
    String name();

    /** {@code true} quando o aluno paga numa página do gateway (redirecionamento). */
    boolean isExternal();

    /** Inicia a cobrança. */
    Checkout start(CheckoutRequest request);

    /** Consulta o pagamento no gateway; vazio se o id não existe lá. */
    Optional<GatewayStatus> lookup(String gatewayPaymentId);

    /**
     * @param externalReference id do nosso {@code GatewayPayment}; volta no retorno e no webhook
     * @param notificationUrl   URL do webhook, ou {@code null} quando não há URL pública
     */
    record CheckoutRequest(String externalReference, String title, BigDecimal amount, PaymentMethod method,
                           String successUrl, String pendingUrl, String failureUrl, String notificationUrl) {
    }

    /**
     * @param redirectUrl  para onde mandar o aluno (gateway externo), ou {@code null}
     * @param preferenceId id da preferência no gateway, se houver
     * @param immediate    resultado já conhecido na hora (pagamento simulado), ou {@code null}
     */
    record Checkout(String redirectUrl, String preferenceId, GatewayStatus immediate) {
    }

    /** Pagamento como o gateway o vê. {@code amount} é o valor cobrado de fato. */
    record GatewayStatus(String gatewayPaymentId, String externalReference, PaymentStatus status, BigDecimal amount) {
    }
}

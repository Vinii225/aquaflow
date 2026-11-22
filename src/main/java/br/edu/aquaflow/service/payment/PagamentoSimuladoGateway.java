package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.domain.enums.PaymentStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pagamento simulado (comportamento das Sprints #2 e #3): aprova na hora, sem sair do site.
 * Usado quando {@code MERCADOPAGO_ACCESS_TOKEN} não está definida (CI, desenvolvimento e
 * homologação sem chave). Guarda os pagamentos em memória para o retorno e o webhook
 * funcionarem igual ao gateway de verdade.
 */
public class PagamentoSimuladoGateway implements GatewayPagamento {

    private final Map<String, GatewayStatus> payments = new ConcurrentHashMap<>();
    private volatile PaymentStatus nextOutcome = PaymentStatus.APPROVED;

    @Override
    public String name() {
        return "SIMULATED";
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    @Override
    public Checkout start(CheckoutRequest request) {
        String id = "SIM-" + UUID.randomUUID();
        GatewayStatus status = new GatewayStatus(id, request.externalReference(), nextOutcome, request.amount());
        payments.put(id, status);
        return new Checkout(null, null, status);
    }

    @Override
    public Optional<GatewayStatus> lookup(String gatewayPaymentId) {
        return Optional.ofNullable(gatewayPaymentId).map(payments::get);
    }

    /** Testes: resultado das próximas cobranças (padrão: aprovado). */
    public void setNextOutcome(PaymentStatus outcome) {
        this.nextOutcome = outcome;
    }

    /** Testes: o "gateway" muda o status depois (ex.: Pix pendente que foi pago). */
    public void changeStatus(String gatewayPaymentId, PaymentStatus status) {
        payments.computeIfPresent(gatewayPaymentId,
                (id, current) -> new GatewayStatus(id, current.externalReference(), status, current.amount()));
    }
}

package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.domain.enums.PaymentStatus;
import com.mercadopago.client.preference.PreferenceRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Monta a requisição do Checkout Pro e traduz os status, sem chamar a API do Mercado Pago. */
class MercadoPagoGatewayTest {

    @Test
    void preferenciaLevaReferenciaValorEUrlsDeRetorno() {
        PreferenceRequest req = MercadoPagoGateway.preferenceRequest(request("https://aquaflow.example"));

        assertThat(req.getExternalReference()).isEqualTo("ref-123");
        assertThat(req.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getUnitPrice()).isEqualByComparingTo("150.00");
            assertThat(item.getQuantity()).isEqualTo(1);
            assertThat(item.getCurrencyId()).isEqualTo("BRL");
        });
        assertThat(req.getBackUrls().getSuccess()).isEqualTo("https://aquaflow.example/pagamentos/retorno/sucesso");
        assertThat(req.getBackUrls().getPending()).isEqualTo("https://aquaflow.example/pagamentos/retorno/pendente");
        assertThat(req.getBackUrls().getFailure()).isEqualTo("https://aquaflow.example/pagamentos/retorno/falha");
        assertThat(req.getAutoReturn()).isEqualTo("approved");
        assertThat(req.getNotificationUrl()).isEqualTo("https://aquaflow.example/pagamentos/webhook/mercadopago");
    }

    @Test
    void semUrlPublicaNaoPedeRetornoAutomaticoNemWebhook() {
        GatewayPagamento.CheckoutRequest local = new GatewayPagamento.CheckoutRequest("ref-123", "Mensalidade",
                new BigDecimal("150.00"), PaymentMethod.PIX, "http://localhost:8080/pagamentos/retorno/sucesso",
                "http://localhost:8080/pagamentos/retorno/pendente", "http://localhost:8080/pagamentos/retorno/falha", null);

        PreferenceRequest req = MercadoPagoGateway.preferenceRequest(local);

        assertThat(req.getAutoReturn()).isNull();
        assertThat(req.getNotificationUrl()).isNull();
    }

    @Test
    void traduzOsStatusDoMercadoPago() {
        assertThat(MercadoPagoGateway.mapStatus("approved")).isEqualTo(PaymentStatus.APPROVED);
        assertThat(MercadoPagoGateway.mapStatus("pending")).isEqualTo(PaymentStatus.PENDING);
        assertThat(MercadoPagoGateway.mapStatus("in_process")).isEqualTo(PaymentStatus.PENDING);
        assertThat(MercadoPagoGateway.mapStatus("rejected")).isEqualTo(PaymentStatus.REJECTED);
        assertThat(MercadoPagoGateway.mapStatus("cancelled")).isEqualTo(PaymentStatus.REJECTED);
        assertThat(MercadoPagoGateway.mapStatus(null)).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void idQueNaoENumeroNaoVaiParaAApi() {
        assertThat(new MercadoPagoGateway("TEST-token").lookup("SIM-abc")).isEmpty();
        assertThat(new MercadoPagoGateway("TEST-token").lookup(null)).isEmpty();
    }

    private static GatewayPagamento.CheckoutRequest request(String base) {
        return new GatewayPagamento.CheckoutRequest("ref-123", "Mensalidade Regular - 11/2026",
                new BigDecimal("150.00"), PaymentMethod.PIX,
                base + "/pagamentos/retorno/sucesso", base + "/pagamentos/retorno/pendente",
                base + "/pagamentos/retorno/falha", base + "/pagamentos/webhook/mercadopago");
    }
}

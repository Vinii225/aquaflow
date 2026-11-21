package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.config.AquaflowProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe o gateway na subida: com {@code MERCADOPAGO_ACCESS_TOKEN} definida, Mercado Pago;
 * sem ela, o pagamento simulado. Assim CI e homologação sem chave seguem funcionando como antes.
 */
@Configuration
public class PaymentGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayConfig.class);

    @Bean
    public GatewayPagamento gatewayPagamento(AquaflowProperties properties) {
        AquaflowProperties.MercadoPago mercadoPago = properties.payment().mercadoPago();
        if (mercadoPago.isEnabled()) {
            log.info("Pagamento online: Mercado Pago (Checkout Pro)");
            return new MercadoPagoGateway(mercadoPago.accessToken().trim());
        }
        log.info("Pagamento online: simulado (MERCADOPAGO_ACCESS_TOKEN não definida)");
        return new PagamentoSimuladoGateway();
    }
}

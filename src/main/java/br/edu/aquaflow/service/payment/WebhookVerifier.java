package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.config.AquaflowProperties;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Confere a assinatura {@code x-signature} das notificações do Mercado Pago com a chave secreta do
 * webhook ({@code MERCADOPAGO_WEBHOOK_SECRET}), usando o validador oficial do SDK (HMAC-SHA256 sobre
 * {@code data.id}, {@code x-request-id} e {@code ts}, com tolerância de horário).
 * <p>
 * Sem chave configurada, a notificação é aceita com um aviso no log: ela só dispara uma consulta
 * ao Mercado Pago, e o status vem sempre da API, nunca do corpo da notificação.
 */
@Component
public class WebhookVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookVerifier.class);

    private final String secret;

    @Autowired
    public WebhookVerifier(AquaflowProperties properties) {
        this(properties.payment().mercadoPago().webhookSecret());
    }

    WebhookVerifier(String secret) {
        this.secret = secret == null || secret.isBlank() ? null : secret.trim();
    }

    public boolean isTrusted(String signature, String requestId, String dataId) {
        if (secret == null) {
            log.warn("Webhook do Mercado Pago sem MERCADOPAGO_WEBHOOK_SECRET: assinatura não conferida");
            return true;
        }
        try {
            WebhookSignatureValidator.validate(signature, requestId, dataId, secret);
            return true;
        } catch (MPInvalidWebhookSignatureException e) {
            log.warn("Webhook do Mercado Pago com assinatura inválida ({}), request-id {}", e.getReason(), requestId);
            return false;
        }
    }
}

package br.edu.aquaflow.web;

import br.edu.aquaflow.domain.enums.PaymentReference;
import br.edu.aquaflow.domain.enums.PaymentStatus;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.payment.PaymentGatewayException;
import br.edu.aquaflow.service.payment.PaymentService;
import br.edu.aquaflow.service.payment.WebhookVerifier;
import br.edu.aquaflow.service.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Retorno do aluno do Mercado Pago (back_urls) e webhook de notificação (S4-06).
 * Nos dois casos o status é consultado na API do gateway; os parâmetros recebidos só dizem
 * qual pagamento consultar.
 */
@Controller
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final Set<String> RETURNS = Set.of("sucesso", "pendente", "falha");

    private final PaymentService paymentService;
    private final StoreService storeService;
    private final WebhookVerifier webhookVerifier;

    public PaymentController(PaymentService paymentService, StoreService storeService, WebhookVerifier webhookVerifier) {
        this.paymentService = paymentService;
        this.storeService = storeService;
        this.webhookVerifier = webhookVerifier;
    }

    /** back_urls: /pagamentos/retorno/{sucesso|pendente|falha}?payment_id=...&external_reference=... */
    @GetMapping("/pagamentos/retorno/{resultado}")
    public String paymentReturn(@PathVariable String resultado,
                                @RequestParam(name = "external_reference", required = false) String externalReference,
                                @RequestParam(name = "payment_id", required = false) String gatewayPaymentId,
                                @AuthenticationPrincipal AquaflowUserDetails currentUser,
                                RedirectAttributes redirectAttributes) {
        UUID paymentId = parseUuid(externalReference);
        if (!RETURNS.contains(resultado) || paymentId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // O Mercado Pago manda payment_id=null quando o aluno volta sem pagar
        String gatewayId = gatewayPaymentId == null || gatewayPaymentId.isBlank() || "null".equals(gatewayPaymentId)
                ? null : gatewayPaymentId;

        PaymentService.Result result;
        try {
            result = paymentService.handleReturn(paymentId, gatewayId, currentUser.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } catch (PaymentGatewayException e) {
            log.warn("Retorno do pagamento {}: falha ao consultar o gateway", paymentId, e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Não conseguimos confirmar o pagamento agora. Ele será atualizado assim que o Mercado Pago avisar.");
            return "redirect:/painel";
        }

        if (result.status() == PaymentStatus.REJECTED && result.type() == PaymentReference.STORE_ORDER) {
            storeService.restoreCart(result.referenceId(), currentUser.getUserId());
        }
        redirectAttributes.addFlashAttribute(result.status() == PaymentStatus.REJECTED ? "errorMessage" : "successMessage",
                message(result.status()));
        return result.type() == PaymentReference.INVOICE
                ? "redirect:/planos/contratacao/" + result.subscriptionId()
                : "redirect:/loja/pedidos/" + result.referenceId();
    }

    /**
     * Webhook do Mercado Pago: {@code POST /pagamentos/webhook/mercadopago?data.id=...&type=payment}.
     * Responde 200 para o que foi processado ou ignorado (o Mercado Pago para de reenviar) e 503
     * quando não deu para consultar a API (ele tenta de novo depois).
     */
    @PostMapping("/pagamentos/webhook/mercadopago")
    @ResponseBody
    public ResponseEntity<Void> mercadoPagoWebhook(@RequestParam(name = "data.id", required = false) String dataId,
                                                   @RequestParam(required = false) String type,
                                                   @RequestHeader(name = "x-signature", required = false) String signature,
                                                   @RequestHeader(name = "x-request-id", required = false) String requestId,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        String eventType = type != null ? type : body == null ? null : (String) body.get("type");
        String paymentId = dataId != null ? dataId : bodyDataId(body);

        if (!webhookVerifier.isTrusted(signature, requestId, dataId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"payment".equals(eventType) || paymentId == null) {
            return ResponseEntity.ok().build();
        }
        try {
            paymentService.refresh(paymentId);
            return ResponseEntity.ok().build();
        } catch (PaymentGatewayException e) {
            log.warn("Webhook: falha ao consultar o pagamento {}", paymentId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    private static String message(PaymentStatus status) {
        return switch (status) {
            case APPROVED -> "Pagamento aprovado!";
            case PENDING -> "Pagamento em análise. Avisaremos aqui assim que for confirmado.";
            case REJECTED -> "O pagamento foi recusado. Nada foi cobrado; você pode tentar de novo.";
        };
    }

    @SuppressWarnings("unchecked")
    private static String bodyDataId(Map<String, Object> body) {
        if (body != null && body.get("data") instanceof Map<?, ?> data && data.get("id") != null) {
            return String.valueOf(((Map<String, Object>) data).get("id"));
        }
        return null;
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package br.edu.aquaflow.service.payment;

import br.edu.aquaflow.domain.enums.PaymentStatus;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import java.util.List;
import java.util.Optional;

/**
 * Mercado Pago Checkout Pro (SDK oficial {@code com.mercadopago:sdk-java}): cria uma preferência de
 * pagamento e manda o aluno para a página do Mercado Pago, que aceita Pix, cartão e boleto.
 * O resultado volta pelas {@code back_urls} e pelo webhook; nos dois casos o status é consultado
 * na API ({@link #lookup}), nunca lido dos parâmetros da URL.
 * <p>
 * O token vai em cada requisição ({@link MPRequestOptions}), sem configuração global.
 */
public class MercadoPagoGateway implements GatewayPagamento {

    private static final int TIMEOUT_MS = 15_000;

    private final String accessToken;

    public MercadoPagoGateway(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String name() {
        return "MERCADO_PAGO";
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    @Override
    public Checkout start(CheckoutRequest request) {
        try {
            Preference preference = new PreferenceClient().create(preferenceRequest(request), options());
            return new Checkout(redirectUrl(preference, accessToken), preference.getId(), null);
        } catch (MPApiException e) {
            throw new PaymentGatewayException(
                    "O Mercado Pago recusou a criação do pagamento (HTTP " + e.getStatusCode() + ")", e);
        } catch (MPException e) {
            throw new PaymentGatewayException("Não foi possível falar com o Mercado Pago", e);
        }
    }

    @Override
    public Optional<GatewayStatus> lookup(String gatewayPaymentId) {
        Long id = parseId(gatewayPaymentId);
        if (id == null) {
            return Optional.empty();
        }
        try {
            Payment payment = new PaymentClient().get(id, options());
            return Optional.of(new GatewayStatus(String.valueOf(payment.getId()), payment.getExternalReference(),
                    mapStatus(payment.getStatus()), payment.getTransactionAmount()));
        } catch (MPApiException e) {
            if (e.getStatusCode() == 404) {
                return Optional.empty();
            }
            throw new PaymentGatewayException("O Mercado Pago recusou a consulta do pagamento (HTTP " + e.getStatusCode() + ")", e);
        } catch (MPException e) {
            throw new PaymentGatewayException("Não foi possível consultar o pagamento no Mercado Pago", e);
        }
    }

    /** Preferência com um item (a fatura ou o pedido inteiro), em reais. */
    static PreferenceRequest preferenceRequest(CheckoutRequest request) {
        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(request.externalReference())
                .title(request.title())
                .quantity(1)
                .unitPrice(request.amount())
                .currencyId("BRL")
                .build();

        PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(List.of(item))
                .externalReference(request.externalReference())
                .statementDescriptor("AQUAFLOW")
                .backUrls(PreferenceBackUrlsRequest.builder()
                        .success(request.successUrl())
                        .pending(request.pendingUrl())
                        .failure(request.failureUrl())
                        .build());
        // Volta sozinho para o site quando aprova. Só com URL pública: o Mercado Pago não aceita localhost nas back_urls
        if (request.successUrl() != null && request.successUrl().startsWith("https://")) {
            builder.autoReturn("approved");
        }
        if (request.notificationUrl() != null) {
            builder.notificationUrl(request.notificationUrl());
        }
        return builder.build();
    }

    /** Status do Mercado Pago para o nosso: aprovado, pendente ou recusado. */
    static PaymentStatus mapStatus(String status) {
        if (status == null) {
            return PaymentStatus.PENDING;
        }
        return switch (status) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected", "cancelled", "refunded", "charged_back" -> PaymentStatus.REJECTED;
            default -> PaymentStatus.PENDING; // pending, in_process, authorized, in_mediation
        };
    }

    /** Credencial antiga de teste ("TEST-...") usa o checkout de sandbox; conta de teste usa o init_point normal. */
    static String redirectUrl(Preference preference, String accessToken) {
        if (accessToken.startsWith("TEST-") && preference.getSandboxInitPoint() != null) {
            return preference.getSandboxInitPoint();
        }
        return preference.getInitPoint() != null ? preference.getInitPoint() : preference.getSandboxInitPoint();
    }

    private MPRequestOptions options() {
        return MPRequestOptions.builder()
                .accessToken(accessToken)
                .connectionTimeout(TIMEOUT_MS)
                .socketTimeout(TIMEOUT_MS)
                .build();
    }

    private static Long parseId(String value) {
        try {
            return value == null ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

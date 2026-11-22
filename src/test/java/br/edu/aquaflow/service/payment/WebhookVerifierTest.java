package br.edu.aquaflow.service.payment;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assinatura do webhook como o Mercado Pago gera: HMAC-SHA256 (hex) do manifesto
 * {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;} com a chave secreta.
 */
class WebhookVerifierTest {

    private static final String SECRET = "chave-de-teste-nao-e-real";

    @Test
    void assinaturaCorretaEAceita() throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String header = "ts=" + ts + ",v1=" + hmac("id:123456;request-id:req-1;ts:" + ts + ";");

        assertThat(new WebhookVerifier(SECRET).isTrusted(header, "req-1", "123456")).isTrue();
    }

    @Test
    void assinaturaDeOutroPagamentoOuSemCabecalhoERecusada() throws Exception {
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String header = "ts=" + ts + ",v1=" + hmac("id:123456;request-id:req-1;ts:" + ts + ";");
        WebhookVerifier verifier = new WebhookVerifier(SECRET);

        assertThat(verifier.isTrusted(header, "req-1", "999999")).isFalse();
        assertThat(verifier.isTrusted(null, "req-1", "123456")).isFalse();
        assertThat(verifier.isTrusted("ts=" + ts + ",v1=00", "req-1", "123456")).isFalse();
    }

    @Test
    void semChaveConfiguradaNaoBloqueia() {
        assertThat(new WebhookVerifier((String) null).isTrusted(null, null, "123")).isTrue();
    }

    private static String hmac(String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }
}

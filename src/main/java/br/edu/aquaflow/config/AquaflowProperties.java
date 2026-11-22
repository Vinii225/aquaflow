package br.edu.aquaflow.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configurações da aplicação (prefixo {@code aquaflow.*} no application.properties).
 * Tipadas e validadas na subida: um valor faltando derruba o boot em vez de quebrar uma tela.
 * Os dados de cada escola (nome, endereço, telefone) ficam na tabela {@code tenants}.
 */
@Validated
@ConfigurationProperties(prefix = "aquaflow")
public record AquaflowProperties(@Valid Booking booking, @Valid Payment payment) {

    public record Booking(@Min(1) int sessionGenerationWeeksAhead) {
    }

    /**
     * Pagamento online (S4-06). Tudo vem de variáveis de ambiente; vazio = pagamento simulado.
     *
     * @param publicBaseUrl URL pública do site (ex.: https://aquaflow-9h8a.onrender.com), usada nas
     *                      back_urls e no webhook. Vazia: usa o endereço da própria requisição e não envia webhook.
     */
    public record Payment(String publicBaseUrl, MercadoPago mercadoPago) {

        public MercadoPago mercadoPago() {
            return mercadoPago == null ? new MercadoPago(null, null) : mercadoPago;
        }
    }

    public record MercadoPago(String accessToken, String webhookSecret) {

        public boolean isEnabled() {
            return accessToken != null && !accessToken.isBlank();
        }
    }
}

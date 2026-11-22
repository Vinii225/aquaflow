package br.edu.aquaflow.web;

import br.edu.aquaflow.config.AquaflowProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endereço público do site, usado nas back_urls e no webhook do pagamento.
 * {@code APP_BASE_URL} tem prioridade (no Render, atrás do proxy, a requisição pode chegar como http);
 * sem ela, usa o endereço da requisição atual.
 */
@Component
public class PublicBaseUrl {

    private final String configured;

    public PublicBaseUrl(AquaflowProperties properties) {
        this.configured = properties.payment().publicBaseUrl();
    }

    public String resolve() {
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}

package br.edu.aquaflow.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configurações da aplicação (prefixo {@code aquaflow.*} no application.properties).
 * Tipadas e validadas na subida: um valor faltando derruba o boot em vez de quebrar uma tela.
 */
@Validated
@ConfigurationProperties(prefix = "aquaflow")
public record AquaflowProperties(@Valid School school, @Valid Booking booking) {

    public record School(@NotBlank String name, @NotBlank String address, @NotBlank String phone) {

        /** Link clicável no celular: "(83) 99999-0000" -> "tel:+5583999990000". */
        public String phoneHref() {
            return "tel:+55" + phone.replaceAll("\\D", "");
        }
    }

    public record Booking(@Min(1) int sessionGenerationWeeksAhead) {
    }
}

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
public record AquaflowProperties(@Valid Booking booking) {

    public record Booking(@Min(1) int sessionGenerationWeeksAhead) {
    }
}

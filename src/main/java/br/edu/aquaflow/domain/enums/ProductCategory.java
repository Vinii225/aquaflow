package br.edu.aquaflow.domain.enums;

import java.util.Arrays;
import java.util.Optional;

public enum ProductCategory {
    ACCESSORY("Acessórios", "acessorios"),
    TRAINING("Equipamentos de treino", "treino"),
    SWIMWEAR("Roupas de banho", "roupas-de-banho");

    private final String label;
    private final String slug;

    ProductCategory(String label, String slug) {
        this.label = label;
        this.slug = slug;
    }

    public String getLabel() {
        return label;
    }

    /** Valor usado na URL da loja ({@code /loja?categoria=treino}). */
    public String getSlug() {
        return slug;
    }

    public static Optional<ProductCategory> fromSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(c -> c.slug.equalsIgnoreCase(slug.trim())).findFirst();
    }
}

package br.edu.aquaflow.domain.enums;

public enum ProductCategory {
    ACCESSORY("Acessórios"),
    TRAINING("Equipamentos de treino"),
    SWIMWEAR("Roupas de banho");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

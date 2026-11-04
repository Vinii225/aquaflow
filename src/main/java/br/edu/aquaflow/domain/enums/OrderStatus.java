package br.edu.aquaflow.domain.enums;

public enum OrderStatus {
    PAID("Pago · retirar na recepção"), DELIVERED("Retirado"), CANCELED("Cancelado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

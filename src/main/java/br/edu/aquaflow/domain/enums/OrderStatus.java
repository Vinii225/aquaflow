package br.edu.aquaflow.domain.enums;

public enum OrderStatus {
    PENDING("Aguardando pagamento"),
    PAID("Pago · retirar na recepção"),
    DELIVERED("Retirado"),
    REJECTED("Pagamento recusado"),
    CANCELED("Cancelado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

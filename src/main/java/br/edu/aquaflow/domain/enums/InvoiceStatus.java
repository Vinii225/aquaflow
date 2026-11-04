package br.edu.aquaflow.domain.enums;

public enum InvoiceStatus {
    OPEN("Em aberto"), PAID("Paga"), OVERDUE("Vencida"), CANCELED("Cancelada");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

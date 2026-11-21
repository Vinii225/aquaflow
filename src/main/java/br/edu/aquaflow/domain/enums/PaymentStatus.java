package br.edu.aquaflow.domain.enums;

/** Situação de um pagamento online, já traduzida do status do gateway. */
public enum PaymentStatus {
    PENDING("Pendente"), APPROVED("Aprovado"), REJECTED("Recusado");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

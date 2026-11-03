package br.edu.aquaflow.domain.enums;

public enum ContactMessageStatus {
    NEW("Nova"), FORWARDED("Encaminhada"), ARCHIVED("Arquivada");

    private final String label;

    ContactMessageStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

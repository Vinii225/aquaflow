package br.edu.aquaflow.domain.enums;

/**
 * Formas de pagamento aceitas na contratação pelo site. O {@code code} é o mesmo texto
 * gravado em {@code transactions.payment_method} pela tela de faturas do administrador.
 */
public enum PaymentMethod {
    PIX("pix", "Pix", "Aprovação na hora"),
    CREDIT_CARD("cartão", "Cartão de crédito", "Cobrança mensal no cartão"),
    BOLETO("boleto", "Boleto", "Compensa em até 3 dias úteis");

    private final String code;
    private final String label;
    private final String hint;

    PaymentMethod(String code, String label, String hint) {
        this.code = code;
        this.label = label;
        this.hint = hint;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getHint() {
        return hint;
    }
}

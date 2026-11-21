package br.edu.aquaflow.domain.enums;

/** O que um pagamento online está pagando. */
public enum PaymentReference {
    /** Mensalidade da contratação de plano (PB-02). */
    INVOICE,
    /** Pedido da loja (PB-04). */
    STORE_ORDER
}

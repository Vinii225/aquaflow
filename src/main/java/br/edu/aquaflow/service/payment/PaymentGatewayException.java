package br.edu.aquaflow.service.payment;

/** O gateway de pagamento não respondeu ou recusou a requisição. */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}

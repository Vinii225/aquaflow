package br.edu.aquaflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentForm {

    @NotBlank(message = "Informe a forma de pagamento")
    private String paymentMethod;
}

package br.edu.aquaflow.web.dto;

import br.edu.aquaflow.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreCheckoutForm {

    @NotNull(message = "Escolha a forma de pagamento")
    private PaymentMethod paymentMethod = PaymentMethod.PIX;
}

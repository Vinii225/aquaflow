package br.edu.aquaflow.web.dto;

import br.edu.aquaflow.domain.enums.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentForm {

    @NotNull(message = "Escolha o dia de vencimento")
    private Integer dueDay = 10;

    @NotNull(message = "Escolha a forma de pagamento")
    private PaymentMethod paymentMethod = PaymentMethod.PIX;

    @AssertTrue(message = "Confirme que leu as condições do plano")
    private boolean acceptTerms;
}

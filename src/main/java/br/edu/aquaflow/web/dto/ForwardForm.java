package br.edu.aquaflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForwardForm {

    @NotBlank(message = "Selecione para quem a mensagem foi encaminhada")
    @Size(max = 64)
    private String forwardedTo;

    @Size(max = 1000, message = "A observação deve ter no máximo 1000 caracteres")
    private String note;
}

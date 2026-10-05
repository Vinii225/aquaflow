package br.edu.aquaflow.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Informe seu nome")
    private String name;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String password;

    @NotBlank(message = "Confirme sua senha")
    private String confirmPassword;
}

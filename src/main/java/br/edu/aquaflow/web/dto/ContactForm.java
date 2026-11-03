package br.edu.aquaflow.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ContactForm {

    private UUID planId;

    @NotBlank(message = "Informe seu nome")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "E-mail inválido")
    @Size(max = 255)
    private String email;

    @Size(max = 32, message = "Telefone muito longo")
    private String phone;

    @NotBlank(message = "Escreva sua mensagem")
    @Size(max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
    private String message;

    // honeypot: campo oculto, preenchido apenas por bots
    private String website;
}

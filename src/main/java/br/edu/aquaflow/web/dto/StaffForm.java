package br.edu.aquaflow.web.dto;

import br.edu.aquaflow.domain.enums.TenantUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffForm {

    @NotBlank(message = "Informe o nome")
    private String name;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String password;

    @NotNull(message = "Selecione o papel")
    private TenantUserRole role;
}

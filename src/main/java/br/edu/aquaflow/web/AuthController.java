package br.edu.aquaflow.web;

import br.edu.aquaflow.service.AuthService;
import br.edu.aquaflow.web.dto.RegisterForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "As senhas não conferem");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.registerStudent(form.getName(), form.getEmail(), form.getPassword());
        } catch (AuthService.EmailAlreadyUsedException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Conta criada com sucesso! Faça login para continuar.");
        return "redirect:/login";
    }
}

package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.domain.TenantUser;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.service.AuthService;
import br.edu.aquaflow.service.TenantContext;
import br.edu.aquaflow.web.dto.StaffForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final TenantUserRepository tenantUserRepository;
    private final AuthService authService;
    private final TenantContext tenantContext;

    public AdminUserController(TenantUserRepository tenantUserRepository,
                                AuthService authService,
                                TenantContext tenantContext) {
        this.tenantUserRepository = tenantUserRepository;
        this.authService = authService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("tenantUsers", tenantUserRepository.findByTenantId(tenantContext.getTenantId()));
        return "admin/users";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("staffForm")) {
            model.addAttribute("staffForm", new StaffForm());
        }
        return "admin/user-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("staffForm") StaffForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/user-form";
        }

        try {
            authService.registerWithRole(form.getName(), form.getEmail(), form.getPassword(), form.getRole());
        } catch (AuthService.EmailAlreadyUsedException e) {
            bindingResult.rejectValue("email", "duplicate", e.getMessage());
            return "admin/user-form";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Usuário cadastrado com sucesso.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        TenantUser tenantUser = tenantUserRepository.findById(id).orElseThrow();
        tenantUser.setStatus(tenantUser.getStatus() == MembershipStatus.ACTIVE
                ? MembershipStatus.SUSPENDED
                : MembershipStatus.ACTIVE);
        tenantUser.setUpdatedAt(Instant.now());
        tenantUserRepository.save(tenantUser);
        redirectAttributes.addFlashAttribute("successMessage", "Status do vínculo atualizado.");
        return "redirect:/admin/users";
    }
}

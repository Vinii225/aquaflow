package br.edu.aquaflow.web;

import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.dashboard.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Painéis do administrador e do professor, e o destino padrão depois do login. */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** Cada papel cai na sua tela principal depois de entrar. */
    @GetMapping("/painel")
    public String home(@AuthenticationPrincipal AquaflowUserDetails currentUser) {
        if (hasRole(currentUser, "SCHOOL_ADMIN")) {
            return "redirect:/admin";
        }
        if (hasRole(currentUser, "TEACHER")) {
            return "redirect:/teacher";
        }
        if (hasRole(currentUser, "PLATFORM_ADMIN")) {
            return "redirect:/plataforma/mensagens";
        }
        return "redirect:/";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("dash", dashboardService.admin());
        return "admin/dashboard";
    }

    @GetMapping("/teacher")
    public String teacher(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        model.addAttribute("dash", dashboardService.teacher(currentUser.getUserId()));
        return "teacher/dashboard";
    }

    private static boolean hasRole(AquaflowUserDetails user, String role) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}

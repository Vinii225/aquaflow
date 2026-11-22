package br.edu.aquaflow.web.platform;

import br.edu.aquaflow.service.platform.SchoolOverviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Escolas da plataforma, vistas pela Equipe AquaFlow. */
@Controller
@RequestMapping("/plataforma/escolas")
public class PlatformSchoolController {

    private final SchoolOverviewService schoolOverviewService;

    public PlatformSchoolController(SchoolOverviewService schoolOverviewService) {
        this.schoolOverviewService = schoolOverviewService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("overview", schoolOverviewService.overview());
        return "platform/schools";
    }
}

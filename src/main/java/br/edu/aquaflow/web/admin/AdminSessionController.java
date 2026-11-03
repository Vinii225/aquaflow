package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.config.AquaflowProperties;
import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.repository.ClassSessionRepository;
import br.edu.aquaflow.service.ScheduleService;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/sessions")
public class AdminSessionController {

    private final ClassSessionRepository classSessionRepository;
    private final ScheduleService scheduleService;
    private final TenantContext tenantContext;

    private final int weeksAhead;

    public AdminSessionController(ClassSessionRepository classSessionRepository,
                                   ScheduleService scheduleService,
                                   TenantContext tenantContext,
                                   AquaflowProperties properties) {
        this.classSessionRepository = classSessionRepository;
        this.scheduleService = scheduleService;
        this.tenantContext = tenantContext;
        this.weeksAhead = properties.booking().sessionGenerationWeeksAhead();
    }

    @GetMapping
    public String list(@PageableDefault(size = 20) Pageable pageable, Model model) {
        Page<ClassSession> sessions = classSessionRepository
                .findByTenantIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                        tenantContext.getTenantId(), LocalDate.now(), pageable);
        model.addAttribute("sessions", sessions);
        model.addAttribute("weeksAhead", weeksAhead);
        return "admin/sessions";
    }

    @PostMapping("/generate")
    public String generate(RedirectAttributes redirectAttributes) {
        int created = scheduleService.generateSessions(weeksAhead);
        redirectAttributes.addFlashAttribute("successMessage",
                created + " aula(s) gerada(s) para as próximas " + weeksAhead + " semanas.");
        return "redirect:/admin/sessions";
    }
}

package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.domain.ClassSchedule;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.repository.ClassScheduleRepository;
import br.edu.aquaflow.repository.SwimClassRepository;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.service.TenantContext;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/schedules")
public class AdminScheduleController {

    private final ClassScheduleRepository classScheduleRepository;
    private final SwimClassRepository swimClassRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantContext tenantContext;

    public AdminScheduleController(ClassScheduleRepository classScheduleRepository,
                                    SwimClassRepository swimClassRepository,
                                    TenantUserRepository tenantUserRepository,
                                    TenantContext tenantContext) {
        this.classScheduleRepository = classScheduleRepository;
        this.swimClassRepository = swimClassRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<ClassSchedule> schedules = classScheduleRepository.findByTenantId(tenantContext.getTenantId(), pageable);
        model.addAttribute("schedules", schedules);
        model.addAttribute("dayNames", List.of("", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"));
        return "admin/schedules";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("classSchedule")) {
            model.addAttribute("classSchedule", new ClassSchedule());
        }
        addOptions(model);
        return "admin/schedule-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("classSchedule", classScheduleRepository.findById(id).orElseThrow());
        addOptions(model);
        return "admin/schedule-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("classSchedule") ClassSchedule classSchedule,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (classSchedule.getStartTime() != null && classSchedule.getEndTime() != null
                && !classSchedule.getEndTime().isAfter(classSchedule.getStartTime())) {
            bindingResult.rejectValue("endTime", "invalid", "O horário de término deve ser depois do início");
        }

        if (bindingResult.hasErrors()) {
            addOptions(model);
            return "admin/schedule-form";
        }

        boolean isNew = classSchedule.getId() == null;
        if (isNew) {
            classSchedule.setTenant(tenantContext.getTenant());
        } else {
            ClassSchedule existing = classScheduleRepository.findById(classSchedule.getId()).orElseThrow();
            classSchedule.setTenant(existing.getTenant());
            classSchedule.setCreatedAt(existing.getCreatedAt());
        }
        classSchedule.setUpdatedAt(Instant.now());
        classScheduleRepository.save(classSchedule);

        redirectAttributes.addFlashAttribute("successMessage", "Horário salvo com sucesso.");
        return "redirect:/admin/schedules";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        classScheduleRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Horário removido.");
        return "redirect:/admin/schedules";
    }

    private void addOptions(Model model) {
        var tenantId = tenantContext.getTenantId();
        model.addAttribute("classes", swimClassRepository.findByTenantIdAndDeletedAtIsNull(tenantId));
        var teachers = tenantUserRepository.findByTenantIdAndRole(tenantId, TenantUserRole.TEACHER).stream()
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .map(tu -> tu.getUser())
                .toList();
        model.addAttribute("teachers", teachers);
        model.addAttribute("dayNames", List.of("", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo"));
    }
}

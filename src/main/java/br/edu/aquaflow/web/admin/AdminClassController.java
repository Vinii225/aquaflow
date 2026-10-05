package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.domain.SwimClass;
import br.edu.aquaflow.repository.PoolRepository;
import br.edu.aquaflow.repository.SwimClassRepository;
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
import java.util.UUID;

@Controller
@RequestMapping("/admin/classes")
public class AdminClassController {

    private final SwimClassRepository swimClassRepository;
    private final PoolRepository poolRepository;
    private final TenantContext tenantContext;

    public AdminClassController(SwimClassRepository swimClassRepository,
                                 PoolRepository poolRepository,
                                 TenantContext tenantContext) {
        this.swimClassRepository = swimClassRepository;
        this.poolRepository = poolRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<SwimClass> classes = swimClassRepository.findByTenantIdAndDeletedAtIsNull(tenantContext.getTenantId(), pageable);
        model.addAttribute("classes", classes);
        return "admin/classes";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("swimClass")) {
            model.addAttribute("swimClass", new SwimClass());
        }
        addPools(model);
        return "admin/class-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("swimClass", swimClassRepository.findById(id).orElseThrow());
        addPools(model);
        return "admin/class-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("swimClass") SwimClass swimClass,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addPools(model);
            return "admin/class-form";
        }

        boolean isNew = swimClass.getId() == null;
        if (isNew) {
            swimClass.setTenant(tenantContext.getTenant());
        } else {
            SwimClass existing = swimClassRepository.findById(swimClass.getId()).orElseThrow();
            swimClass.setTenant(existing.getTenant());
            swimClass.setCreatedAt(existing.getCreatedAt());
        }
        swimClass.setUpdatedAt(Instant.now());
        swimClassRepository.save(swimClass);

        redirectAttributes.addFlashAttribute("successMessage", "Turma salva com sucesso.");
        return "redirect:/admin/classes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        SwimClass swimClass = swimClassRepository.findById(id).orElseThrow();
        swimClass.setDeletedAt(Instant.now());
        swimClassRepository.save(swimClass);
        redirectAttributes.addFlashAttribute("successMessage", "Turma removida.");
        return "redirect:/admin/classes";
    }

    private void addPools(Model model) {
        model.addAttribute("pools", poolRepository.findByTenantIdAndDeletedAtIsNull(tenantContext.getTenantId()));
    }
}

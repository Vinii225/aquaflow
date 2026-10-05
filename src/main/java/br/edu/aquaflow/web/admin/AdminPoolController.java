package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.domain.Pool;
import br.edu.aquaflow.repository.PoolRepository;
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
@RequestMapping("/admin/pools")
public class AdminPoolController {

    private final PoolRepository poolRepository;
    private final TenantContext tenantContext;

    public AdminPoolController(PoolRepository poolRepository, TenantContext tenantContext) {
        this.poolRepository = poolRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<Pool> pools = poolRepository.findByTenantIdAndDeletedAtIsNull(tenantContext.getTenantId(), pageable);
        model.addAttribute("pools", pools);
        return "admin/pools";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("pool")) {
            model.addAttribute("pool", new Pool());
        }
        return "admin/pool-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        Pool pool = poolRepository.findById(id).orElseThrow();
        model.addAttribute("pool", pool);
        return "admin/pool-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("pool") Pool pool,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/pool-form";
        }

        boolean isNew = pool.getId() == null;
        if (isNew) {
            pool.setTenant(tenantContext.getTenant());
        } else {
            Pool existing = poolRepository.findById(pool.getId()).orElseThrow();
            pool.setTenant(existing.getTenant());
            pool.setCreatedAt(existing.getCreatedAt());
        }
        pool.setUpdatedAt(Instant.now());
        poolRepository.save(pool);

        redirectAttributes.addFlashAttribute("successMessage", "Piscina salva com sucesso.");
        return "redirect:/admin/pools";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        Pool pool = poolRepository.findById(id).orElseThrow();
        pool.setDeletedAt(Instant.now());
        poolRepository.save(pool);
        redirectAttributes.addFlashAttribute("successMessage", "Piscina removida.");
        return "redirect:/admin/pools";
    }
}

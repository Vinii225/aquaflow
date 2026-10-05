package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.domain.Subscription;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.repository.SubscriptionRepository;
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
import java.util.UUID;

@Controller
@RequestMapping("/admin/subscriptions")
public class AdminSubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantUserRepository tenantUserRepository;
    private final TenantContext tenantContext;

    public AdminSubscriptionController(SubscriptionRepository subscriptionRepository,
                                        TenantUserRepository tenantUserRepository,
                                        TenantContext tenantContext) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(@PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<Subscription> subscriptions = subscriptionRepository.findByTenantId(tenantContext.getTenantId(), pageable);
        model.addAttribute("subscriptions", subscriptions);
        return "admin/subscriptions";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("subscription")) {
            model.addAttribute("subscription", new Subscription());
        }
        addStudents(model);
        return "admin/subscription-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable UUID id, Model model) {
        model.addAttribute("subscription", subscriptionRepository.findById(id).orElseThrow());
        addStudents(model);
        return "admin/subscription-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("subscription") Subscription subscription,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addStudents(model);
            return "admin/subscription-form";
        }

        boolean isNew = subscription.getId() == null;
        if (isNew) {
            subscription.setTenant(tenantContext.getTenant());
        } else {
            Subscription existing = subscriptionRepository.findById(subscription.getId()).orElseThrow();
            subscription.setTenant(existing.getTenant());
            subscription.setCreatedAt(existing.getCreatedAt());
        }
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);

        redirectAttributes.addFlashAttribute("successMessage", "Assinatura salva com sucesso.");
        return "redirect:/admin/subscriptions";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        Subscription subscription = subscriptionRepository.findById(id).orElseThrow();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);
        redirectAttributes.addFlashAttribute("successMessage", "Assinatura cancelada.");
        return "redirect:/admin/subscriptions";
    }

    private void addStudents(Model model) {
        var students = tenantUserRepository.findByTenantIdAndRole(tenantContext.getTenantId(), TenantUserRole.STUDENT).stream()
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .map(tu -> tu.getUser())
                .toList();
        model.addAttribute("students", students);
    }
}

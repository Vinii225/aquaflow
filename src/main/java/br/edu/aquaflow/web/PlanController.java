package br.edu.aquaflow.web;

import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.UserRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.ContactMessageService;
import br.edu.aquaflow.service.TenantContext;
import br.edu.aquaflow.web.dto.ContactForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PlanController {

    private final TenantContext tenantContext;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final ContactMessageService contactMessageService;

    public PlanController(TenantContext tenantContext,
                           PlanRepository planRepository,
                           UserRepository userRepository,
                           ContactMessageService contactMessageService) {
        this.tenantContext = tenantContext;
        this.planRepository = planRepository;
        this.userRepository = userRepository;
        this.contactMessageService = contactMessageService;
    }

    @GetMapping("/planos")
    public String index(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        ContactForm form = new ContactForm();
        if (currentUser != null) {
            userRepository.findById(currentUser.getUserId()).ifPresent(u -> {
                form.setName(u.getName());
                form.setEmail(u.getEmail());
                form.setPhone(u.getPhone());
            });
        }
        model.addAttribute("contactForm", form);
        return page(model);
    }

    @PostMapping("/planos/contato")
    public String contact(@Valid @ModelAttribute("contactForm") ContactForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal AquaflowUserDetails currentUser,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("openContactModal", true);
            return page(model);
        }

        if (form.getWebsite() == null || form.getWebsite().isBlank()) {
            contactMessageService.send(form, currentUser != null ? currentUser.getUserId() : null);
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Mensagem enviada! Nossa equipe vai encaminhá-la à escola e você receberá o retorno em breve.");
        return "redirect:/planos";
    }

    private String page(Model model) {
        model.addAttribute("plans",
                planRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantContext.getTenantId()));
        return "plans";
    }
}

package br.edu.aquaflow.web;

import br.edu.aquaflow.domain.Plan;
import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.PlanEnrollmentService;
import br.edu.aquaflow.service.payment.PaymentGatewayException;
import br.edu.aquaflow.service.payment.PaymentService;
import br.edu.aquaflow.web.dto.EnrollmentForm;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Contratação de plano pelo aluno (PB-02). Acesso restrito a alunos em {@code SecurityConfig}. */
@Controller
public class PlanEnrollmentController {

    private final PlanEnrollmentService enrollmentService;
    private final PaymentService paymentService;
    private final PublicBaseUrl publicBaseUrl;

    public PlanEnrollmentController(PlanEnrollmentService enrollmentService,
                                    PaymentService paymentService,
                                    PublicBaseUrl publicBaseUrl) {
        this.enrollmentService = enrollmentService;
        this.paymentService = paymentService;
        this.publicBaseUrl = publicBaseUrl;
    }

    @GetMapping("/planos/{planId}/contratar")
    public String checkout(@PathVariable UUID planId,
                           @AuthenticationPrincipal AquaflowUserDetails currentUser,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        var currentPlan = enrollmentService.currentPlanName(currentUser.getUserId());
        if (currentPlan.isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Você já tem o plano " + currentPlan.get() + " ativo. Para trocar de plano, fale com a escola.");
            return "redirect:/planos";
        }
        model.addAttribute("enrollmentForm", new EnrollmentForm());
        return page(planId, model);
    }

    @PostMapping("/planos/{planId}/contratar")
    public String enroll(@PathVariable UUID planId,
                         @Valid @ModelAttribute("enrollmentForm") EnrollmentForm form,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal AquaflowUserDetails currentUser,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (form.getDueDay() != null && !PlanEnrollmentService.DUE_DAYS.contains(form.getDueDay())) {
            bindingResult.rejectValue("dueDay", "invalid", "Escolha um dos dias de vencimento");
        }
        if (bindingResult.hasErrors()) {
            return page(planId, model);
        }

        try {
            var enrollment = enrollmentService.enroll(currentUser.getUserId(), planId, form.getDueDay(),
                    form.getPaymentMethod(), publicBaseUrl.resolve());
            if (enrollment.paymentRedirectUrl() != null) {
                return "redirect:" + enrollment.paymentRedirectUrl();
            }
            return "redirect:/planos/contratacao/" + enrollment.subscriptionId();
        } catch (PlanEnrollmentService.AlreadySubscribedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/planos";
        } catch (PaymentGatewayException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Não foi possível iniciar o pagamento agora. Nada foi cobrado; tente de novo em instantes.");
            return "redirect:/planos/" + planId + "/contratar";
        }
    }

    @GetMapping("/planos/contratacao/{subscriptionId}")
    public String confirmation(@PathVariable UUID subscriptionId,
                               @AuthenticationPrincipal AquaflowUserDetails currentUser,
                               Model model) {
        model.addAttribute("confirmation", enrollmentService.confirmation(subscriptionId, currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "plans/confirmation";
    }

    private String page(UUID planId, Model model) {
        Plan plan = enrollmentService.findAvailablePlan(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("plan", plan);
        model.addAttribute("dueDays", PlanEnrollmentService.DUE_DAYS);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("onlinePayment", paymentService.isExternalGateway());
        return "plans/checkout";
    }
}

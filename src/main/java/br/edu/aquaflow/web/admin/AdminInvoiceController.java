package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.domain.Invoice;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.BillingService;
import br.edu.aquaflow.service.TenantContext;
import br.edu.aquaflow.web.dto.PaymentForm;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.UUID;

@Controller
@RequestMapping("/admin/invoices")
public class AdminInvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final BillingService billingService;
    private final TenantContext tenantContext;

    public AdminInvoiceController(InvoiceRepository invoiceRepository,
                                   BillingService billingService,
                                   TenantContext tenantContext) {
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(@PageableDefault(size = 15) Pageable pageable, Model model) {
        Page<Invoice> invoices = invoiceRepository.findByTenantIdOrderByDueDateDesc(tenantContext.getTenantId(), pageable);
        model.addAttribute("invoices", invoices);
        model.addAttribute("paymentForm", new PaymentForm());
        return "admin/invoices";
    }

    @PostMapping("/generate")
    public String generate(RedirectAttributes redirectAttributes) {
        int created = billingService.generateInvoicesForMonth(YearMonth.now());
        redirectAttributes.addFlashAttribute("successMessage",
                created + " fatura(s) gerada(s) para o mês atual.");
        return "redirect:/admin/invoices";
    }

    @PostMapping("/{id}/pay")
    public String pay(@PathVariable UUID id,
                       @Valid @ModelAttribute("paymentForm") PaymentForm form,
                       BindingResult bindingResult,
                       @AuthenticationPrincipal AquaflowUserDetails currentUser,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Informe a forma de pagamento.");
            return "redirect:/admin/invoices";
        }

        billingService.registerPayment(id, form.getPaymentMethod(), currentUser.getUserId());
        redirectAttributes.addFlashAttribute("successMessage", "Pagamento registrado.");
        return "redirect:/admin/invoices";
    }
}

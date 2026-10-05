package br.edu.aquaflow.web.student;

import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student/invoices")
public class StudentInvoiceController {

    private final InvoiceRepository invoiceRepository;

    public StudentInvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public String myInvoices(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        model.addAttribute("invoices", invoiceRepository.findByStudentId(currentUser.getUserId()));
        return "student/invoices";
    }
}

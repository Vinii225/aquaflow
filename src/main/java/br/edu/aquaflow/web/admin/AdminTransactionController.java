package br.edu.aquaflow.web.admin;

import br.edu.aquaflow.repository.TransactionRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/transactions")
public class AdminTransactionController {

    private final TransactionRepository transactionRepository;
    private final TenantContext tenantContext;

    public AdminTransactionController(TransactionRepository transactionRepository, TenantContext tenantContext) {
        this.transactionRepository = transactionRepository;
        this.tenantContext = tenantContext;
    }

    @GetMapping
    public String list(@PageableDefault(size = 20) Pageable pageable, Model model) {
        model.addAttribute("transactions",
                transactionRepository.findByTenantIdOrderByPaidAtDesc(tenantContext.getTenantId(), pageable));
        return "admin/transactions";
    }
}

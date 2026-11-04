package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.Invoice;
import br.edu.aquaflow.domain.Subscription;
import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.domain.Transaction;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.domain.enums.TransactionType;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.SubscriptionRepository;
import br.edu.aquaflow.repository.TransactionRepository;
import br.edu.aquaflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class BillingService {

    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TenantContext tenantContext;

    public BillingService(SubscriptionRepository subscriptionRepository,
                           InvoiceRepository invoiceRepository,
                           TransactionRepository transactionRepository,
                           UserRepository userRepository,
                           TenantContext tenantContext) {
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public int generateInvoicesForMonth(YearMonth month) {
        var tenantId = tenantContext.getTenantId();
        LocalDate referenceMonth = month.atDay(1);
        int created = 0;

        for (Subscription subscription : subscriptionRepository.findByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)) {
            boolean exists = invoiceRepository
                    .findBySubscriptionIdAndReferenceMonth(subscription.getId(), referenceMonth)
                    .isPresent();
            if (exists) {
                continue;
            }

            int lastDay = month.lengthOfMonth();
            int day = Math.min(subscription.getDueDay(), lastDay);

            Invoice invoice = new Invoice();
            invoice.setTenant(subscription.getTenant());
            invoice.setSubscription(subscription);
            invoice.setReferenceMonth(referenceMonth);
            invoice.setAmount(subscription.getMonthlyFee());
            invoice.setDueDate(month.atDay(day));
            invoice.setStatus(InvoiceStatus.OPEN);
            invoiceRepository.save(invoice);
            created++;
        }
        return created;
    }

    @Transactional
    public void registerPayment(UUID invoiceId, String paymentMethod, UUID registeredByUserId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Fatura não encontrada"));
        registerIncome(invoice.getTenant(), "tuition", "invoice", invoice.getId(),
                invoice.getAmount(), paymentMethod, registeredByUserId);

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        invoiceRepository.save(invoice);
    }

    /** Lança uma entrada no caixa (mensalidade, venda da loja...). */
    @Transactional
    public void registerIncome(Tenant tenant, String category, String referenceType, UUID referenceId,
                               BigDecimal amount, String paymentMethod, UUID registeredByUserId) {
        User registeredBy = userRepository.findById(registeredByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        Transaction transaction = new Transaction();
        transaction.setTenant(tenant);
        transaction.setType(TransactionType.INCOME);
        transaction.setCategory(category);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setAmount(amount);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaidAt(Instant.now());
        transaction.setCreatedBy(registeredBy);
        transactionRepository.save(transaction);
    }
}

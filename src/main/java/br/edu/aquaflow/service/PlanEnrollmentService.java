package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.Invoice;
import br.edu.aquaflow.domain.Plan;
import br.edu.aquaflow.domain.Subscription;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.SubscriptionRepository;
import br.edu.aquaflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contratação de plano pelo próprio aluno (PB-02): cria a assinatura, gera a fatura do mês
 * e registra o pagamento dela no caixa. O pagamento ainda é simulado: o time não definiu o
 * meio de pagamento real, então a primeira mensalidade é dada como paga na hora.
 */
@Service
public class PlanEnrollmentService {

    public static final List<Integer> DUE_DAYS = List.of(5, 10, 15, 20);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final BillingService billingService;
    private final TenantContext tenantContext;

    public PlanEnrollmentService(PlanRepository planRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 InvoiceRepository invoiceRepository,
                                 UserRepository userRepository,
                                 BillingService billingService,
                                 TenantContext tenantContext) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.billingService = billingService;
        this.tenantContext = tenantContext;
    }

    public record Confirmation(String planName, BigDecimal monthlyFee, int dueDay,
                               LocalDate startDate, LocalDate nextDueDate, String firstInvoiceStatus) {
    }

    public static class AlreadySubscribedException extends RuntimeException {
        private final String planName;

        public AlreadySubscribedException(String planName) {
            super("Você já tem o plano " + planName + " ativo.");
            this.planName = planName;
        }

        public String getPlanName() {
            return planName;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Plan> findAvailablePlan(UUID planId) {
        UUID tenantId = tenantContext.getTenantId();
        return planRepository.findById(planId)
                .filter(Plan::isActive)
                .filter(p -> p.getTenant().getId().equals(tenantId));
    }

    @Transactional(readOnly = true)
    public Optional<String> currentPlanName(UUID studentId) {
        return activeSubscription(studentId).map(Subscription::getPlanName);
    }

    @Transactional
    public UUID enroll(UUID studentId, UUID planId, int dueDay, PaymentMethod paymentMethod) {
        if (!DUE_DAYS.contains(dueDay)) {
            throw new IllegalArgumentException("Dia de vencimento inválido");
        }
        Plan plan = findAvailablePlan(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));
        activeSubscription(studentId).ifPresent(s -> {
            throw new AlreadySubscribedException(s.getPlanName());
        });
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        LocalDate today = LocalDate.now();

        Subscription subscription = new Subscription();
        subscription.setTenant(plan.getTenant());
        subscription.setStudent(student);
        subscription.setPlanName(plan.getName());
        subscription.setMonthlyFee(plan.getMonthlyFee());
        subscription.setDueDay((short) dueDay);
        subscription.setStartDate(today);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscriptionRepository.save(subscription);

        // A primeira mensalidade é cobrada na contratação; as seguintes vencem no dia escolhido.
        Invoice invoice = new Invoice();
        invoice.setTenant(plan.getTenant());
        invoice.setSubscription(subscription);
        invoice.setReferenceMonth(YearMonth.from(today).atDay(1));
        invoice.setAmount(plan.getMonthlyFee());
        invoice.setDueDate(today);
        invoice.setStatus(InvoiceStatus.OPEN);
        invoiceRepository.save(invoice);

        billingService.registerPayment(invoice.getId(), paymentMethod.getCode(), studentId);
        return subscription.getId();
    }

    @Transactional(readOnly = true)
    public Optional<Confirmation> confirmation(UUID subscriptionId, UUID studentId) {
        return subscriptionRepository.findById(subscriptionId)
                .filter(s -> s.getStudent().getId().equals(studentId))
                .map(s -> new Confirmation(
                        s.getPlanName(),
                        s.getMonthlyFee(),
                        s.getDueDay(),
                        s.getStartDate(),
                        nextDueDate(s.getStartDate(), s.getDueDay()),
                        invoiceRepository.findBySubscriptionIdOrderByReferenceMonthDesc(s.getId()).stream()
                                .findFirst()
                                .map(i -> i.getStatus().getLabel())
                                .orElse(null)));
    }

    private Optional<Subscription> activeSubscription(UUID studentId) {
        return subscriptionRepository.findFirstByStudentIdAndStatusOrderByStartDateDesc(studentId, SubscriptionStatus.ACTIVE);
    }

    /** Vencimento da segunda mensalidade: o dia escolhido no mês seguinte ao da contratação. */
    static LocalDate nextDueDate(LocalDate startDate, int dueDay) {
        YearMonth next = YearMonth.from(startDate).plusMonths(1);
        return next.atDay(Math.min(dueDay, next.lengthOfMonth()));
    }
}

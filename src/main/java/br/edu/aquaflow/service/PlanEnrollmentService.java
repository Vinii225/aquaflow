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
import br.edu.aquaflow.service.payment.PaymentService;
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
 * e cobra a primeira mensalidade pelo {@link PaymentService} (Mercado Pago em sandbox ou
 * pagamento simulado). Aprovado: fatura paga. Pendente: fatura em aberto. Recusado: fatura e
 * assinatura canceladas, e o aluno pode tentar de novo.
 */
@Service
public class PlanEnrollmentService {

    public static final List<Integer> DUE_DAYS = List.of(5, 10, 15, 20);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final TenantContext tenantContext;

    public PlanEnrollmentService(PlanRepository planRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 InvoiceRepository invoiceRepository,
                                 UserRepository userRepository,
                                 PaymentService paymentService,
                                 TenantContext tenantContext) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.tenantContext = tenantContext;
    }

    public record Confirmation(String planName, BigDecimal monthlyFee, int dueDay,
                               LocalDate startDate, LocalDate nextDueDate, InvoiceStatus firstInvoice) {

        public String getFirstInvoiceStatus() {
            return firstInvoice == null ? null : firstInvoice.getLabel();
        }

        public boolean isPaid() {
            return firstInvoice == InvoiceStatus.PAID;
        }

        public boolean isRejected() {
            return firstInvoice == InvoiceStatus.CANCELED;
        }
    }

    /** Assinatura criada e, se o pagamento é no gateway, para onde mandar o aluno. */
    public record Enrollment(UUID subscriptionId, String paymentRedirectUrl) {
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

    /** @param baseUrl endereço público do site, para o gateway devolver o aluno e notificar */
    @Transactional
    public Enrollment enroll(UUID studentId, UUID planId, int dueDay, PaymentMethod paymentMethod, String baseUrl) {
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

        PaymentService.Start payment = paymentService.startInvoice(invoice, paymentMethod, student, baseUrl);
        return new Enrollment(subscription.getId(), payment.redirectUrl());
    }

    @Transactional(readOnly = true)
    public Optional<Confirmation> confirmation(UUID subscriptionId, UUID studentId) {
        return subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantContext.getTenantId())
                .filter(s -> s.getStudent().getId().equals(studentId))
                .map(s -> new Confirmation(
                        s.getPlanName(),
                        s.getMonthlyFee(),
                        s.getDueDay(),
                        s.getStartDate(),
                        nextDueDate(s.getStartDate(), s.getDueDay()),
                        invoiceRepository.findBySubscriptionIdOrderByReferenceMonthDesc(s.getId()).stream()
                                .findFirst()
                                .map(Invoice::getStatus)
                                .orElse(null)));
    }

    private Optional<Subscription> activeSubscription(UUID studentId) {
        return subscriptionRepository.findFirstByTenantIdAndStudentIdAndStatusOrderByStartDateDesc(
                tenantContext.getTenantId(), studentId, SubscriptionStatus.ACTIVE);
    }

    /** Vencimento da segunda mensalidade: o dia escolhido no mês seguinte ao da contratação. */
    static LocalDate nextDueDate(LocalDate startDate, int dueDay) {
        YearMonth next = YearMonth.from(startDate).plusMonths(1);
        return next.atDay(Math.min(dueDay, next.lengthOfMonth()));
    }
}

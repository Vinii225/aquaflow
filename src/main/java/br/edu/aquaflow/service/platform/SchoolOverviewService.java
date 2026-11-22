package br.edu.aquaflow.service.platform;

import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.domain.Transaction;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.domain.enums.TenantStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.domain.enums.TransactionType;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.PoolRepository;
import br.edu.aquaflow.repository.ProductRepository;
import br.edu.aquaflow.repository.SubscriptionRepository;
import br.edu.aquaflow.repository.SwimClassRepository;
import br.edu.aquaflow.repository.TenantRepository;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Visão da Equipe AquaFlow sobre todas as escolas da plataforma.
 * É a única leitura que passa por várias escolas, e cada número é contado com o {@code tenant_id} da própria escola.
 */
@Service
public class SchoolOverviewService {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final TenantRepository tenantRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PoolRepository poolRepository;
    private final SwimClassRepository swimClassRepository;
    private final PlanRepository planRepository;
    private final ProductRepository productRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;

    public SchoolOverviewService(TenantRepository tenantRepository,
                                 TenantUserRepository tenantUserRepository,
                                 PoolRepository poolRepository,
                                 SwimClassRepository swimClassRepository,
                                 PlanRepository planRepository,
                                 ProductRepository productRepository,
                                 SubscriptionRepository subscriptionRepository,
                                 InvoiceRepository invoiceRepository,
                                 TransactionRepository transactionRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.poolRepository = poolRepository;
        this.swimClassRepository = swimClassRepository;
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
    }

    public record SchoolSummary(Tenant school, List<String> adminEmails,
                                long students, long teachers, long pools, long classes,
                                long plans, long products, long activeSubscriptions,
                                long openInvoices, BigDecimal monthRevenue) {

        public boolean isActive() {
            return school.getStatus() == TenantStatus.ACTIVE;
        }
    }

    public record Overview(List<SchoolSummary> schools) {

        public long getActiveSchools() {
            return schools.stream().filter(SchoolSummary::isActive).count();
        }

        public long getStudents() {
            return schools.stream().mapToLong(SchoolSummary::students).sum();
        }

        public long getActiveSubscriptions() {
            return schools.stream().mapToLong(SchoolSummary::activeSubscriptions).sum();
        }

        public BigDecimal getMonthRevenue() {
            return schools.stream().map(SchoolSummary::monthRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        var monthStart = LocalDate.now(ZONE).withDayOfMonth(1).atStartOfDay(ZONE).toInstant();
        List<SchoolSummary> schools = tenantRepository.findByDeletedAtIsNullOrderByDefaultSchoolDescCreatedAtAsc().stream()
                .map(school -> summary(school, monthStart))
                .toList();
        return new Overview(schools);
    }

    private SchoolSummary summary(Tenant school, Instant monthStart) {
        UUID id = school.getId();
        List<String> admins = tenantUserRepository.findActiveUsersByRole(id, TenantUserRole.SCHOOL_ADMIN).stream()
                .map(User::getEmail)
                .sorted()
                .toList();
        BigDecimal revenue = transactionRepository.findByTypeSince(id, TransactionType.INCOME, monthStart).stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SchoolSummary(school, admins,
                tenantUserRepository.countByTenantIdAndRoleAndStatus(id, TenantUserRole.STUDENT, MembershipStatus.ACTIVE),
                tenantUserRepository.countByTenantIdAndRoleAndStatus(id, TenantUserRole.TEACHER, MembershipStatus.ACTIVE),
                poolRepository.countByTenantIdAndDeletedAtIsNull(id),
                swimClassRepository.countByTenantIdAndDeletedAtIsNull(id),
                planRepository.countByTenantIdAndActiveTrue(id),
                productRepository.countByTenantIdAndActiveTrue(id),
                subscriptionRepository.countByTenantIdAndStatus(id, SubscriptionStatus.ACTIVE),
                invoiceRepository.countByTenantIdAndStatus(id, InvoiceStatus.OPEN),
                revenue);
    }
}

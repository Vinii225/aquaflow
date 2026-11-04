package br.edu.aquaflow.service.dashboard;

import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.domain.StoreOrder;
import br.edu.aquaflow.domain.Transaction;
import br.edu.aquaflow.domain.enums.AttendanceStatus;
import br.edu.aquaflow.domain.enums.BookingStatus;
import br.edu.aquaflow.domain.enums.InvoiceStatus;
import br.edu.aquaflow.domain.enums.SessionStatus;
import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import br.edu.aquaflow.domain.enums.TransactionType;
import br.edu.aquaflow.repository.AttendanceRepository;
import br.edu.aquaflow.repository.ClassSessionRepository;
import br.edu.aquaflow.repository.InvoiceRepository;
import br.edu.aquaflow.repository.StoreOrderRepository;
import br.edu.aquaflow.repository.StudentBookingRepository;
import br.edu.aquaflow.repository.SubscriptionRepository;
import br.edu.aquaflow.repository.TransactionRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Números dos painéis do administrador e do professor. Só leitura. */
@Service
public class DashboardService {

    static final int REVENUE_MONTHS = 6;
    static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    public record MonthRevenue(YearMonth month, String label, BigDecimal tuition, BigDecimal store) {
        public BigDecimal getTotal() {
            return tuition.add(store);
        }
    }

    /** Receita por mês com as alturas das barras já calculadas (0–100% do maior mês). */
    public record RevenueChart(List<MonthRevenue> months, BigDecimal max) {
        public int heightOf(BigDecimal value) {
            if (max.signum() == 0 || value.signum() == 0) {
                return 0;
            }
            return Math.max(1, value.multiply(BigDecimal.valueOf(100)).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue());
        }

        public boolean isEmpty() {
            return max.signum() == 0;
        }
    }

    public record SessionLoad(ClassSession session, int booked, int capacity) {
        public int getPercent() {
            return capacity <= 0 ? 0 : Math.min(100, Math.round(booked * 100f / capacity));
        }

        public boolean isFull() {
            return capacity > 0 && booked >= capacity;
        }
    }

    public record AdminDashboard(long activeStudents, BigDecimal monthRevenue, BigDecimal monthStore,
                                 long openInvoices, BigDecimal openAmount, long overdueInvoices,
                                 RevenueChart revenue, List<SessionLoad> upcoming, int weekOccupancy,
                                 List<StoreOrder> recentOrders) {
    }

    public record TeacherDashboard(List<SessionLoad> today, List<SessionLoad> week,
                                   List<SessionLoad> pendingAttendance, int studentsThisWeek,
                                   Integer attendanceRate, long attendanceRecorded) {
    }

    private final TenantContext tenantContext;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final ClassSessionRepository sessionRepository;
    private final StudentBookingRepository bookingRepository;
    private final AttendanceRepository attendanceRepository;
    private final StoreOrderRepository orderRepository;

    public DashboardService(TenantContext tenantContext,
                            SubscriptionRepository subscriptionRepository,
                            InvoiceRepository invoiceRepository,
                            TransactionRepository transactionRepository,
                            ClassSessionRepository sessionRepository,
                            StudentBookingRepository bookingRepository,
                            AttendanceRepository attendanceRepository,
                            StoreOrderRepository orderRepository) {
        this.tenantContext = tenantContext;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.sessionRepository = sessionRepository;
        this.bookingRepository = bookingRepository;
        this.attendanceRepository = attendanceRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboard admin() {
        UUID tenantId = tenantContext.getTenantId();
        LocalDate today = LocalDate.now(ZONE);
        YearMonth current = YearMonth.from(today);

        YearMonth first = current.minusMonths(REVENUE_MONTHS - 1);
        var income = transactionRepository.findByTypeSince(tenantId, TransactionType.INCOME,
                first.atDay(1).atStartOfDay(ZONE).toInstant());
        RevenueChart chart = revenueByMonth(income, current, ZONE);
        MonthRevenue thisMonth = chart.months().get(chart.months().size() - 1);

        List<SessionLoad> upcoming = loads(sessionRepository.findByTenantIdAndDateBetweenAndStatusNotOrderByDateAscStartTimeAsc(
                tenantId, today, today.plusDays(6), SessionStatus.CANCELED));

        return new AdminDashboard(
                subscriptionRepository.countByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE),
                thisMonth.getTotal(),
                thisMonth.store(),
                invoiceRepository.countByTenantIdAndStatus(tenantId, InvoiceStatus.OPEN),
                invoiceRepository.sumAmountByStatus(tenantId, InvoiceStatus.OPEN),
                invoiceRepository.countByTenantIdAndStatusAndDueDateBefore(tenantId, InvoiceStatus.OPEN, today),
                chart,
                upcoming,
                occupancy(upcoming),
                orderRepository.findTop5ByTenantIdOrderByCreatedAtDesc(tenantId));
    }

    @Transactional(readOnly = true)
    public TeacherDashboard teacher(UUID teacherId) {
        LocalDate today = LocalDate.now(ZONE);
        LocalTime now = LocalTime.now(ZONE);

        List<SessionLoad> week = loads(sessionRepository.findByTeacherIdAndDateBetweenAndStatusOrderByDateAscStartTimeAsc(
                teacherId, today, today.plusDays(6), SessionStatus.SCHEDULED));
        List<SessionLoad> todays = week.stream().filter(l -> l.session().getDate().equals(today)).toList();

        // Aula que já começou (ou de dias anteriores) e continua "agendada" = chamada não feita
        List<SessionLoad> pending = loads(sessionRepository.findByTeacherIdAndDateBetweenAndStatusOrderByDateAscStartTimeAsc(
                teacherId, today.minusDays(30), today, SessionStatus.SCHEDULED)).stream()
                .filter(l -> l.session().getDate().isBefore(today) || !l.session().getStartTime().isAfter(now))
                .filter(l -> l.booked() > 0)
                .toList();

        Map<AttendanceStatus, Long> attendance = new HashMap<>();
        for (Object[] row : attendanceRepository.countByStatusForTeacherSince(teacherId, today.minusDays(30))) {
            attendance.put((AttendanceStatus) row[0], (Long) row[1]);
        }
        long recorded = attendance.values().stream().mapToLong(Long::longValue).sum();
        Integer rate = recorded == 0 ? null
                : Math.round(attendance.getOrDefault(AttendanceStatus.PRESENT, 0L) * 100f / recorded);

        return new TeacherDashboard(todays, week, pending,
                week.stream().mapToInt(SessionLoad::booked).sum(), rate, recorded);
    }

    private List<SessionLoad> loads(List<ClassSession> sessions) {
        if (sessions.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> booked = new HashMap<>();
        Collection<UUID> ids = sessions.stream().map(ClassSession::getId).toList();
        for (Object[] row : bookingRepository.countBySessionIds(ids, BookingStatus.CONFIRMED)) {
            booked.put((UUID) row[0], (Long) row[1]);
        }
        return sessions.stream()
                .map(s -> new SessionLoad(s, booked.getOrDefault(s.getId(), 0L).intValue(), capacityOf(s)))
                .toList();
    }

    private static int capacityOf(ClassSession s) {
        if (s.getLimitStudents() != null) {
            return s.getLimitStudents();
        }
        Integer max = s.getSwimClass().getMaxCapacity();
        return max == null ? 0 : max;
    }

    static int occupancy(List<SessionLoad> loads) {
        int capacity = loads.stream().mapToInt(SessionLoad::capacity).sum();
        int booked = loads.stream().mapToInt(l -> Math.min(l.booked(), l.capacity())).sum();
        return capacity == 0 ? 0 : Math.round(booked * 100f / capacity);
    }

    /** Soma as entradas por mês (mensalidade x loja), preenchendo meses sem movimento com zero. */
    static RevenueChart revenueByMonth(List<Transaction> income, YearMonth current, ZoneId zone) {
        Map<YearMonth, BigDecimal[]> sums = new HashMap<>();
        for (Transaction t : income) {
            YearMonth m = YearMonth.from(t.getPaidAt().atZone(zone));
            BigDecimal[] pair = sums.computeIfAbsent(m, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int slot = "store".equals(t.getCategory()) ? 1 : 0;
            pair[slot] = pair[slot].add(t.getAmount());
        }
        List<MonthRevenue> months = new ArrayList<>();
        BigDecimal max = BigDecimal.ZERO;
        for (int i = REVENUE_MONTHS - 1; i >= 0; i--) {
            YearMonth m = current.minusMonths(i);
            BigDecimal[] pair = sums.getOrDefault(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            String label = m.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
            MonthRevenue r = new MonthRevenue(m, label, pair[0], pair[1]);
            months.add(r);
            max = max.max(r.getTotal());
        }
        return new RevenueChart(months, max);
    }
}

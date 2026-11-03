package br.edu.aquaflow.service.site;

import br.edu.aquaflow.domain.ClassSchedule;
import br.edu.aquaflow.domain.Plan;
import br.edu.aquaflow.domain.Pool;
import br.edu.aquaflow.domain.SwimClass;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.domain.enums.Weekday;
import br.edu.aquaflow.repository.ClassScheduleRepository;
import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.PoolRepository;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Monta os dados da página inicial pública a partir da grade, dos planos e da equipe da escola. */
@Service
public class HomePageService {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    /** Tudo o que a home exibe, já no formato da tela. */
    public record HomePage(List<ClassCard> classes, List<TeacherCard> teachers, List<Plan> plans,
                           BigDecimal minPlanFee, int weeklyClasses, List<Pool> pools) {
    }

    /** Turma como o visitante procura: o que é, onde, com quem e quando. */
    public record ClassCard(String name, String level, String pool, Integer capacity,
                            List<String> teachers, List<Slot> slots) {
    }

    /** Dias que compartilham o mesmo horário: "Terça e Quinta" · "07:00 – 08:00". */
    public record Slot(String days, String time) {
    }

    public record TeacherCard(String name, String initials, List<String> classes) {
    }

    private final TenantContext tenantContext;
    private final ClassScheduleRepository classScheduleRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PoolRepository poolRepository;
    private final PlanRepository planRepository;

    public HomePageService(TenantContext tenantContext,
                           ClassScheduleRepository classScheduleRepository,
                           TenantUserRepository tenantUserRepository,
                           PoolRepository poolRepository,
                           PlanRepository planRepository) {
        this.tenantContext = tenantContext;
        this.classScheduleRepository = classScheduleRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.poolRepository = poolRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public HomePage load() {
        UUID tenantId = tenantContext.getTenantId();

        List<ClassSchedule> schedules = classScheduleRepository.findByTenantId(tenantId).stream()
                .sorted(Comparator.comparing(ClassSchedule::getDayOfWeek).thenComparing(ClassSchedule::getStartTime))
                .toList();
        List<User> teachers = tenantUserRepository.findActiveUsersByRole(tenantId, TenantUserRole.TEACHER);
        List<Plan> plans = planRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId);
        BigDecimal minPlanFee = plans.stream().map(Plan::getMonthlyFee).min(Comparator.naturalOrder()).orElse(null);

        return new HomePage(
                toClassCards(schedules),
                toTeacherCards(teachers, schedules),
                plans,
                minPlanFee,
                schedules.size(),
                poolRepository.findByTenantIdAndDeletedAtIsNull(tenantId));
    }

    private List<ClassCard> toClassCards(List<ClassSchedule> schedules) {
        Map<UUID, List<ClassSchedule>> byClass = new LinkedHashMap<>();
        schedules.forEach(s -> byClass.computeIfAbsent(s.getSwimClass().getId(), id -> new ArrayList<>()).add(s));

        return byClass.values().stream().map(classSchedules -> {
            SwimClass swimClass = classSchedules.get(0).getSwimClass();
            List<String> teacherNames = classSchedules.stream()
                    .map(s -> s.getTeacher().getName())
                    .distinct()
                    .toList();
            return new ClassCard(
                    swimClass.getName(),
                    levelIfNotInName(swimClass),
                    swimClass.getPool().getName(),
                    swimClass.getMaxCapacity(),
                    teacherNames,
                    groupSlots(classSchedules));
        }).toList();
    }

    /** Agrupa os dias que têm o mesmo horário, preservando a ordem da semana. */
    static List<Slot> groupSlots(List<ClassSchedule> schedules) {
        Map<String, List<String>> daysByTime = new LinkedHashMap<>();
        schedules.forEach(s -> daysByTime
                .computeIfAbsent(s.getStartTime().format(HOUR) + " – " + s.getEndTime().format(HOUR), t -> new ArrayList<>())
                .add(Weekday.of(s.getDayOfWeek()).getLabel()));
        return daysByTime.entrySet().stream()
                .map(e -> new Slot(joinWithAnd(e.getValue()), e.getKey()))
                .toList();
    }

    private List<TeacherCard> toTeacherCards(List<User> teachers, List<ClassSchedule> schedules) {
        return teachers.stream().map(teacher -> {
            var classes = new LinkedHashSet<String>();
            schedules.stream()
                    .filter(s -> s.getTeacher().getId().equals(teacher.getId()))
                    .forEach(s -> classes.add(s.getSwimClass().getName()));
            return new TeacherCard(teacher.getName(), initials(teacher.getName()), List.copyOf(classes));
        }).toList();
    }

    /** Evita repetir "Iniciante" quando o nome da turma já diz o nível. */
    private static String levelIfNotInName(SwimClass swimClass) {
        String level = swimClass.getLevel();
        if (level == null || level.isBlank()) return null;
        return swimClass.getName().toLowerCase().contains(level.toLowerCase()) ? null : capitalize(level);
    }

    static String joinWithAnd(List<String> items) {
        if (items.size() <= 1) return String.join("", items);
        return String.join(", ", items.subList(0, items.size() - 1)) + " e " + items.get(items.size() - 1);
    }

    static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}

package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.ClassSchedule;
import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.repository.ClassScheduleRepository;
import br.edu.aquaflow.repository.ClassSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class ScheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassSessionRepository classSessionRepository;
    private final TenantContext tenantContext;

    public ScheduleService(ClassScheduleRepository classScheduleRepository,
                            ClassSessionRepository classSessionRepository,
                            TenantContext tenantContext) {
        this.classScheduleRepository = classScheduleRepository;
        this.classSessionRepository = classSessionRepository;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public int generateSessions(int weeksAhead) {
        var tenantId = tenantContext.getTenantId();
        LocalDate today = LocalDate.now();
        LocalDate horizon = today.plusWeeks(weeksAhead);

        int created = 0;
        for (ClassSchedule schedule : classScheduleRepository.findByTenantId(tenantId)) {
            DayOfWeek targetDay = DayOfWeek.of(schedule.getDayOfWeek());
            LocalDate date = nextOrSame(today, targetDay);

            while (!date.isAfter(horizon)) {
                boolean exists = classSessionRepository
                        .findByClassScheduleIdAndDate(schedule.getId(), date)
                        .isPresent();

                if (!exists) {
                    ClassSession session = new ClassSession();
                    session.setTenant(schedule.getTenant());
                    session.setSwimClass(schedule.getSwimClass());
                    session.setClassSchedule(schedule);
                    session.setTeacher(schedule.getTeacher());
                    session.setDate(date);
                    session.setStartTime(schedule.getStartTime());
                    session.setEndTime(schedule.getEndTime());
                    session.setLimitStudents(schedule.getSwimClass().getMaxCapacity());
                    classSessionRepository.save(session);
                    created++;
                }

                date = date.plusWeeks(1);
            }
        }
        return created;
    }

    private LocalDate nextOrSame(LocalDate from, DayOfWeek dayOfWeek) {
        int diff = dayOfWeek.getValue() - from.getDayOfWeek().getValue();
        if (diff < 0) {
            diff += 7;
        }
        return from.plusDays(diff);
    }
}

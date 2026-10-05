package br.edu.aquaflow.web;

import br.edu.aquaflow.domain.enums.MembershipStatus;
import br.edu.aquaflow.domain.enums.TenantUserRole;
import br.edu.aquaflow.repository.ClassScheduleRepository;
import br.edu.aquaflow.repository.PoolRepository;
import br.edu.aquaflow.repository.TenantUserRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
public class HomeController {

    private final TenantContext tenantContext;
    private final ClassScheduleRepository classScheduleRepository;
    private final TenantUserRepository tenantUserRepository;
    private final PoolRepository poolRepository;

    @Value("${aquaflow.school.name}")
    private String schoolName;

    @Value("${aquaflow.school.address}")
    private String schoolAddress;

    @Value("${aquaflow.school.phone}")
    private String schoolPhone;

    public HomeController(TenantContext tenantContext,
                           ClassScheduleRepository classScheduleRepository,
                           TenantUserRepository tenantUserRepository,
                           PoolRepository poolRepository) {
        this.tenantContext = tenantContext;
        this.classScheduleRepository = classScheduleRepository;
        this.tenantUserRepository = tenantUserRepository;
        this.poolRepository = poolRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        var tenantId = tenantContext.getTenantId();

        var schedules = classScheduleRepository.findByTenantId(tenantId).stream()
                .sorted(Comparator.comparing(s -> s.getDayOfWeek() + "-" + s.getStartTime()))
                .toList();

        var teachers = tenantUserRepository.findByTenantIdAndRole(tenantId, TenantUserRole.TEACHER).stream()
                .filter(tu -> tu.getStatus() == MembershipStatus.ACTIVE)
                .map(tu -> tu.getUser())
                .toList();

        List<String> dayNames = List.of("", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado", "Domingo");

        model.addAttribute("schoolName", schoolName);
        model.addAttribute("schoolAddress", schoolAddress);
        model.addAttribute("schoolPhone", schoolPhone);
        model.addAttribute("pools", poolRepository.findByTenantIdAndDeletedAtIsNull(tenantId));
        model.addAttribute("schedules", schedules);
        model.addAttribute("dayNames", dayNames);
        model.addAttribute("teachers", teachers);
        return "home";
    }
}

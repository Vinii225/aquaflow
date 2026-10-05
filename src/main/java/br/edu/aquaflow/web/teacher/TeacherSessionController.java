package br.edu.aquaflow.web.teacher;

import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.domain.enums.AttendanceStatus;
import br.edu.aquaflow.domain.enums.BookingStatus;
import br.edu.aquaflow.repository.AttendanceRepository;
import br.edu.aquaflow.repository.ClassSessionRepository;
import br.edu.aquaflow.repository.StudentBookingRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.AttendanceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/teacher/sessions")
public class TeacherSessionController {

    private final ClassSessionRepository classSessionRepository;
    private final StudentBookingRepository studentBookingRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;

    public TeacherSessionController(ClassSessionRepository classSessionRepository,
                                     StudentBookingRepository studentBookingRepository,
                                     AttendanceRepository attendanceRepository,
                                     AttendanceService attendanceService) {
        this.classSessionRepository = classSessionRepository;
        this.studentBookingRepository = studentBookingRepository;
        this.attendanceRepository = attendanceRepository;
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        LocalDate today = LocalDate.now();
        var sessions = classSessionRepository.findByTeacherIdAndDateBetweenOrderByDateAscStartTimeAsc(
                currentUser.getUserId(), today, today.plusWeeks(2));
        model.addAttribute("sessions", sessions);
        model.addAttribute("today", today);
        return "teacher/sessions";
    }

    @GetMapping("/{id}/attendance")
    public String attendance(@PathVariable UUID id, Model model) {
        ClassSession session = classSessionRepository.findById(id).orElseThrow();
        var bookings = studentBookingRepository.findByClassSessionIdAndStatus(id, BookingStatus.CONFIRMED);

        Map<UUID, AttendanceStatus> recorded = new HashMap<>();
        for (var booking : bookings) {
            attendanceRepository.findByStudentBookingId(booking.getId())
                    .ifPresent(a -> recorded.put(booking.getId(), a.getStatus()));
        }

        model.addAttribute("session", session);
        model.addAttribute("bookings", bookings);
        model.addAttribute("recorded", recorded);
        model.addAttribute("statuses", AttendanceStatus.values());
        return "teacher/attendance";
    }

    @PostMapping("/{id}/attendance")
    public String submitAttendance(@PathVariable UUID id,
                                    @RequestParam Map<String, String> formParams,
                                    @AuthenticationPrincipal AquaflowUserDetails currentUser,
                                    RedirectAttributes redirectAttributes) {
        formParams.forEach((key, value) -> {
            if (key.startsWith("status_") && value != null && !value.isBlank()) {
                UUID bookingId = UUID.fromString(key.substring("status_".length()));
                attendanceService.record(bookingId, AttendanceStatus.valueOf(value), currentUser.getUserId());
            }
        });

        attendanceService.completeSession(id);
        redirectAttributes.addFlashAttribute("successMessage", "Chamada registrada e aula concluída.");
        return "redirect:/teacher/sessions";
    }
}

package br.edu.aquaflow.web.student;

import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.domain.enums.BookingStatus;
import br.edu.aquaflow.domain.enums.SessionStatus;
import br.edu.aquaflow.repository.ClassSessionRepository;
import br.edu.aquaflow.repository.StudentBookingRepository;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.BookingService;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
public class StudentBookingController {

    private final ClassSessionRepository classSessionRepository;
    private final StudentBookingRepository studentBookingRepository;
    private final BookingService bookingService;
    private final TenantContext tenantContext;

    public StudentBookingController(ClassSessionRepository classSessionRepository,
                                     StudentBookingRepository studentBookingRepository,
                                     BookingService bookingService,
                                     TenantContext tenantContext) {
        this.classSessionRepository = classSessionRepository;
        this.studentBookingRepository = studentBookingRepository;
        this.bookingService = bookingService;
        this.tenantContext = tenantContext;
    }

    public record SessionView(ClassSession session, long confirmed, boolean full, boolean alreadyBooked) {}

    @GetMapping("/student/sessions")
    public String availableSessions(@AuthenticationPrincipal AquaflowUserDetails currentUser,
                                     @PageableDefault(size = 20) Pageable pageable,
                                     Model model) {
        var page = classSessionRepository.findByTenantIdAndDateGreaterThanEqualOrderByDateAscStartTimeAsc(
                tenantContext.getTenantId(), LocalDate.now(), pageable);

        List<SessionView> views = page.getContent().stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                .map(s -> {
                    long confirmed = studentBookingRepository.countByClassSessionIdAndStatus(s.getId(), BookingStatus.CONFIRMED);
                    boolean alreadyBooked = studentBookingRepository
                            .findByStudentIdAndClassSessionId(currentUser.getUserId(), s.getId())
                            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                            .isPresent();
                    return new SessionView(s, confirmed, confirmed >= s.getLimitStudents(), alreadyBooked);
                })
                .toList();

        model.addAttribute("sessions", views);
        return "student/sessions";
    }

    @PostMapping("/student/sessions/{id}/book")
    public String book(@PathVariable UUID id,
                        @AuthenticationPrincipal AquaflowUserDetails currentUser,
                        RedirectAttributes redirectAttributes) {
        try {
            bookingService.book(currentUser.getUserId(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Vaga reservada com sucesso!");
        } catch (BookingService.SessionFullException | BookingService.AlreadyBookedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/student/sessions";
    }

    @GetMapping("/student/bookings")
    public String myBookings(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        model.addAttribute("bookings", studentBookingRepository.findByStudentIdOrderByBookedAtDesc(currentUser.getUserId()));
        return "student/bookings";
    }

    @PostMapping("/student/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable UUID id,
                                 @AuthenticationPrincipal AquaflowUserDetails currentUser,
                                 RedirectAttributes redirectAttributes) {
        bookingService.cancel(id, currentUser.getUserId());
        redirectAttributes.addFlashAttribute("successMessage", "Reserva cancelada.");
        return "redirect:/student/bookings";
    }
}

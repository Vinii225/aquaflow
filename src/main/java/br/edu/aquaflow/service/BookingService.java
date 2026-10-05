package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.domain.StudentBooking;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.BookingStatus;
import br.edu.aquaflow.repository.ClassSessionRepository;
import br.edu.aquaflow.repository.StudentBookingRepository;
import br.edu.aquaflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class BookingService {

    private final StudentBookingRepository studentBookingRepository;
    private final ClassSessionRepository classSessionRepository;
    private final UserRepository userRepository;

    public BookingService(StudentBookingRepository studentBookingRepository,
                           ClassSessionRepository classSessionRepository,
                           UserRepository userRepository) {
        this.studentBookingRepository = studentBookingRepository;
        this.classSessionRepository = classSessionRepository;
        this.userRepository = userRepository;
    }

    public static class SessionFullException extends RuntimeException {
        public SessionFullException() {
            super("Não há mais vagas para esta aula");
        }
    }

    public static class AlreadyBookedException extends RuntimeException {
        public AlreadyBookedException() {
            super("Você já tem uma reserva para esta aula");
        }
    }

    @Transactional
    public StudentBooking book(UUID studentId, UUID sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Aula não encontrada"));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        studentBookingRepository.findByStudentIdAndClassSessionId(studentId, sessionId)
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .ifPresent(b -> { throw new AlreadyBookedException(); });

        long confirmed = studentBookingRepository.countByClassSessionIdAndStatus(sessionId, BookingStatus.CONFIRMED);
        if (confirmed >= session.getLimitStudents()) {
            throw new SessionFullException();
        }

        StudentBooking booking = new StudentBooking();
        booking.setTenant(session.getTenant());
        booking.setStudent(student);
        booking.setClassSession(session);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(Instant.now());
        return studentBookingRepository.save(booking);
    }

    @Transactional
    public void cancel(UUID bookingId, UUID studentId) {
        StudentBooking booking = studentBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));
        if (!booking.getStudent().getId().equals(studentId)) {
            throw new IllegalStateException("Esta reserva não pertence a este aluno");
        }
        booking.setStatus(BookingStatus.CANCELED);
        booking.setCanceledAt(Instant.now());
        studentBookingRepository.save(booking);
    }
}

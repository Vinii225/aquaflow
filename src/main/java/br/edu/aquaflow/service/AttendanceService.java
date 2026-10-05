package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.Attendance;
import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.domain.StudentBooking;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.AttendanceStatus;
import br.edu.aquaflow.domain.enums.SessionStatus;
import br.edu.aquaflow.repository.AttendanceRepository;
import br.edu.aquaflow.repository.ClassSessionRepository;
import br.edu.aquaflow.repository.StudentBookingRepository;
import br.edu.aquaflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentBookingRepository studentBookingRepository;
    private final ClassSessionRepository classSessionRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                              StudentBookingRepository studentBookingRepository,
                              ClassSessionRepository classSessionRepository,
                              UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentBookingRepository = studentBookingRepository;
        this.classSessionRepository = classSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(UUID bookingId, AttendanceStatus status, UUID recordedByUserId) {
        StudentBooking booking = studentBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));
        User recordedBy = userRepository.findById(recordedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        Attendance attendance = attendanceRepository.findByStudentBookingId(bookingId)
                .orElseGet(Attendance::new);
        attendance.setTenant(booking.getTenant());
        attendance.setStudentBooking(booking);
        attendance.setStatus(status);
        attendance.setRecordedBy(recordedBy);
        attendance.setRecordedAt(Instant.now());
        attendanceRepository.save(attendance);
    }

    @Transactional
    public void completeSession(UUID sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Aula não encontrada"));
        session.setStatus(SessionStatus.COMPLETED);
        classSessionRepository.save(session);
    }
}

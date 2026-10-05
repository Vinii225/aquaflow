package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.BookingStatus;
import br.edu.aquaflow.domain.enums.BookingType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "student_bookings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "class_session_id"})
})
public class StudentBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_session_id", nullable = false)
    private ClassSession classSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType type = BookingType.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt = Instant.now();

    @Column(name = "canceled_at")
    private Instant canceledAt;
}

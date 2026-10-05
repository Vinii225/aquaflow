package br.edu.aquaflow.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "class_schedules")
public class ClassSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull(message = "Selecione a turma")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SwimClass swimClass;

    @NotNull(message = "Selecione o professor")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Min(1)
    @Max(7)
    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @NotNull(message = "Informe o horário de início")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull(message = "Informe o horário de término")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

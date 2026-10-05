package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.SubscriptionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull(message = "Selecione o aluno")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotBlank(message = "Informe o nome do plano")
    @Column(name = "plan_name", nullable = false)
    private String planName;

    @NotNull(message = "Informe o valor mensal")
    @DecimalMin(value = "0.0", message = "O valor deve ser maior ou igual a zero")
    @Column(name = "monthly_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Min(1)
    @Max(31)
    @Column(name = "due_day", nullable = false)
    private Short dueDay;

    @NotNull(message = "Informe a data de início")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

package br.edu.aquaflow.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank(message = "Informe o nome do plano")
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Informe o valor mensal")
    @DecimalMin(value = "0.0", message = "O valor deve ser maior ou igual a zero")
    @Column(name = "monthly_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyFee;

    @Min(1)
    @Max(7)
    @Column(name = "classes_per_week", nullable = false)
    private Short classesPerWeek;

    @Column(nullable = false)
    private boolean highlighted;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id"))
    @OrderColumn(name = "feature_order")
    @Column(name = "description", nullable = false)
    private List<String> features = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

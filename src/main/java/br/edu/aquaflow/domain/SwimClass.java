package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.ClassStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "classes")
public class SwimClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotNull(message = "Selecione a piscina")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @NotBlank(message = "Informe o nome da turma")
    @Column(nullable = false)
    private String name;

    private String level;

    @Min(value = 1, message = "A capacidade deve ser maior que zero")
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassStatus status = ClassStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

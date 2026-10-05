package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.PoolStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "pools")
public class Pool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank(message = "Informe o nome da piscina")
    @Column(nullable = false)
    private String name;

    @Min(value = 1, message = "A quantidade de raias deve ser maior que zero")
    @Column(name = "total_lanes", nullable = false)
    private Integer totalLanes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PoolStatus status = PoolStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

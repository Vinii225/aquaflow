package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.ProductCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank(message = "Informe o nome do produto")
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductCategory category;

    @NotNull(message = "Informe o preço")
    @DecimalMin(value = "0.0", message = "O preço deve ser maior ou igual a zero")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "image_credit")
    private String imageCredit;

    @Column(name = "display_order", nullable = false)
    private Short displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

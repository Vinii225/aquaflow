package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.OrderStatus;
import jakarta.persistence.*;
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
@Table(name = "store_orders")
public class StoreOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PAID;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_percent", nullable = false)
    private Short discountPercent = 0;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "item_order")
    private List<StoreOrderItem> items = new ArrayList<>();

    public void addItem(StoreOrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public int getItemCount() {
        return items.stream().mapToInt(StoreOrderItem::getQuantity).sum();
    }

    /** Código curto para o aluno informar na recepção. */
    public String getCode() {
        return id == null ? "" : id.toString().substring(0, 8).toUpperCase();
    }
}

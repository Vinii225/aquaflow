package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.PaymentReference;
import br.edu.aquaflow.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma tentativa de pagamento online (Mercado Pago ou simulado) de uma fatura ou de um pedido.
 * O {@code id} vai como {@code external_reference} para o gateway.
 */
@Getter
@Setter
@Entity
@Table(name = "gateway_payments")
public class GatewayPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 20)
    private PaymentReference referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    /** {@code SIMULATED} ou {@code MERCADO_PAGO}. */
    @Column(nullable = false, length = 20)
    private String gateway;

    @Column(name = "gateway_preference_id", length = 100)
    private String gatewayPreferenceId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

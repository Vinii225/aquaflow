package br.edu.aquaflow.service.store;

import br.edu.aquaflow.domain.Plan;
import br.edu.aquaflow.domain.Product;
import br.edu.aquaflow.domain.StoreOrder;
import br.edu.aquaflow.domain.StoreOrderItem;
import br.edu.aquaflow.domain.User;
import br.edu.aquaflow.domain.enums.OrderStatus;
import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.ProductRepository;
import br.edu.aquaflow.repository.StoreOrderRepository;
import br.edu.aquaflow.repository.UserRepository;
import br.edu.aquaflow.domain.enums.PaymentStatus;
import br.edu.aquaflow.service.PlanEnrollmentService;
import br.edu.aquaflow.service.TenantContext;
import br.edu.aquaflow.service.payment.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carrinho e checkout da loja (PB-04). O desconto vem do plano ativo do aluno
 * ({@code plans.product_discount_percent}). O pedido nasce aguardando pagamento e é cobrado pelo
 * {@link PaymentService} (Mercado Pago em sandbox ou simulado); aprovado, fica pago para retirada
 * na recepção.
 */
@Service
public class StoreService {

    public record CartLine(Product product, int quantity, BigDecimal lineTotal) {
    }

    public record CartView(List<CartLine> lines, BigDecimal subtotal, String discountPlanName,
                           int discountPercent, BigDecimal discount, BigDecimal total) {

        public boolean isEmpty() {
            return lines.isEmpty();
        }

        public int getItemCount() {
            return lines.stream().mapToInt(CartLine::quantity).sum();
        }
    }

    /** Pedido criado e, se o pagamento é no gateway, para onde mandar o aluno. */
    public record CheckoutResult(UUID orderId, String paymentRedirectUrl) {
    }

    public static class EmptyCartException extends RuntimeException {
        public EmptyCartException() {
            super("Seu carrinho está vazio.");
        }
    }

    private final ShoppingCart cart;
    private final ProductRepository productRepository;
    private final PlanRepository planRepository;
    private final StoreOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PlanEnrollmentService enrollmentService;
    private final PaymentService paymentService;
    private final TenantContext tenantContext;

    public StoreService(ShoppingCart cart,
                        ProductRepository productRepository,
                        PlanRepository planRepository,
                        StoreOrderRepository orderRepository,
                        UserRepository userRepository,
                        PlanEnrollmentService enrollmentService,
                        PaymentService paymentService,
                        TenantContext tenantContext) {
        this.cart = cart;
        this.productRepository = productRepository;
        this.planRepository = planRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.enrollmentService = enrollmentService;
        this.paymentService = paymentService;
        this.tenantContext = tenantContext;
    }

    public int cartCount() {
        return cart.count();
    }

    @Transactional(readOnly = true)
    public boolean addToCart(UUID productId, int quantity) {
        if (findAvailable(List.of(productId)).isEmpty()) {
            return false;
        }
        cart.add(productId, Math.max(1, quantity));
        return true;
    }

    public void updateQuantity(UUID productId, int quantity) {
        cart.setQuantity(productId, quantity);
    }

    public void removeFromCart(UUID productId) {
        cart.remove(productId);
    }

    /** Monta o carrinho com preços atuais; produtos desativados somem sozinhos. */
    @Transactional(readOnly = true)
    public CartView view(UUID studentId) {
        Map<UUID, Integer> items = cart.items();
        Map<UUID, Product> products = findAvailable(items.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartLine> lines = new ArrayList<>();
        items.forEach((id, qty) -> {
            Product p = products.get(id);
            if (p != null) {
                lines.add(new CartLine(p, qty, p.getPrice().multiply(BigDecimal.valueOf(qty))));
            }
        });
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Optional<Plan> discountPlan = studentId == null ? Optional.empty() : discountPlanOf(studentId);
        int percent = discountPlan.map(p -> (int) p.getProductDiscountPercent()).orElse(0);
        BigDecimal discount = discountOf(subtotal, percent);
        return new CartView(lines, subtotal, discountPlan.map(Plan::getName).orElse(null),
                percent, discount, subtotal.subtract(discount));
    }

    /** @param baseUrl endereço público do site, para o gateway devolver o aluno e notificar */
    @Transactional
    public CheckoutResult checkout(UUID studentId, PaymentMethod paymentMethod, String baseUrl) {
        CartView view = view(studentId);
        if (view.isEmpty()) {
            throw new EmptyCartException();
        }
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado"));

        StoreOrder order = new StoreOrder();
        order.setTenant(tenantContext.getTenant());
        order.setStudent(student);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(view.subtotal());
        order.setDiscountPercent((short) view.discountPercent());
        order.setDiscountAmount(view.discount());
        order.setTotal(view.total());
        order.setPaymentMethod(paymentMethod.getCode());
        for (CartLine line : view.lines()) {
            StoreOrderItem item = new StoreOrderItem();
            item.setProduct(line.product());
            item.setProductName(line.product().getName());
            item.setUnitPrice(line.product().getPrice());
            item.setQuantity((short) line.quantity());
            item.setLineTotal(line.lineTotal());
            order.addItem(item);
        }
        orderRepository.save(order);

        // A entrada no caixa é lançada pelo PaymentService quando o pagamento é aprovado
        PaymentService.Start payment = paymentService.startOrder(order, paymentMethod, student, baseUrl);
        if (payment.status() != PaymentStatus.REJECTED) {
            cart.clear();
        }
        return new CheckoutResult(order.getId(), payment.redirectUrl());
    }

    /** Pagamento recusado no gateway: os itens do pedido voltam para o carrinho. */
    @Transactional(readOnly = true)
    public void restoreCart(UUID orderId, UUID studentId) {
        findOrder(orderId, studentId)
                .filter(o -> o.getStatus() == OrderStatus.REJECTED)
                .ifPresent(o -> o.getItems().stream()
                        .filter(i -> i.getProduct() != null)
                        .forEach(i -> cart.add(i.getProduct().getId(), i.getQuantity())));
    }

    @Transactional(readOnly = true)
    public Optional<StoreOrder> findOrder(UUID orderId, UUID studentId) {
        return orderRepository.findByIdAndTenantId(orderId, tenantContext.getTenantId())
                .filter(o -> o.getStudent().getId().equals(studentId));
    }

    @Transactional(readOnly = true)
    public List<StoreOrder> ordersOf(UUID studentId) {
        return orderRepository.findByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantContext.getTenantId(), studentId);
    }

    private List<Product> findAvailable(Collection<UUID> ids) {
        UUID tenantId = tenantContext.getTenantId();
        return productRepository.findAllById(ids).stream()
                .filter(Product::isActive)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .toList();
    }

    private Optional<Plan> discountPlanOf(UUID studentId) {
        return enrollmentService.currentPlanName(studentId)
                .flatMap(name -> planRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantContext.getTenantId())
                        .stream().filter(p -> p.getName().equals(name)).findFirst())
                .filter(Plan::hasProductDiscount);
    }

    static BigDecimal discountOf(BigDecimal subtotal, int percent) {
        if (percent <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return subtotal.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

package br.edu.aquaflow.service.site;

import br.edu.aquaflow.domain.Plan;
import br.edu.aquaflow.domain.Product;
import br.edu.aquaflow.domain.enums.ProductCategory;
import br.edu.aquaflow.repository.PlanRepository;
import br.edu.aquaflow.repository.ProductRepository;
import br.edu.aquaflow.service.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Catálogo público da loja (PB-03, listagem). Compra e carrinho ficam para o PB-04. */
@Service
public class ProductCatalogService {

    /** Produtos agrupados por categoria (na ordem do enum) e os descontos de aluno por plano. */
    public record Catalog(Map<ProductCategory, List<Product>> productsByCategory, List<PlanDiscount> planDiscounts) {

        public boolean isEmpty() {
            return productsByCategory.isEmpty();
        }
    }

    public record PlanDiscount(String planName, int percent) {
    }

    private final TenantContext tenantContext;
    private final ProductRepository productRepository;
    private final PlanRepository planRepository;

    public ProductCatalogService(TenantContext tenantContext,
                                 ProductRepository productRepository,
                                 PlanRepository planRepository) {
        this.tenantContext = tenantContext;
        this.productRepository = productRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public Catalog load() {
        UUID tenantId = tenantContext.getTenantId();

        Map<ProductCategory, List<Product>> byCategory = productRepository
                .findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId).stream()
                .collect(Collectors.groupingBy(Product::getCategory,
                        () -> new EnumMap<>(ProductCategory.class), Collectors.toList()));

        List<PlanDiscount> discounts = planRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId).stream()
                .filter(Plan::hasProductDiscount)
                .map(p -> new PlanDiscount(p.getName(), p.getProductDiscountPercent()))
                .toList();

        return new Catalog(byCategory, discounts);
    }
}

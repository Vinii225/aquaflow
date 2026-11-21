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

/** Catálogo público da loja (PB-03): listagem, busca e filtros. Só produtos da escola atual. */
@Service
public class ProductCatalogService {

    /**
     * Produtos agrupados por categoria (na ordem do enum), os descontos de aluno por plano e o filtro aplicado.
     * {@code total} conta os produtos ativos da escola antes do filtro.
     */
    public record Catalog(Map<ProductCategory, List<Product>> productsByCategory, List<PlanDiscount> planDiscounts,
                          ProductFilter filter, int found, int total) {

        public boolean isEmpty() {
            return productsByCategory.isEmpty();
        }

        /** Há produtos na loja, mas nenhum passou no filtro. */
        public boolean isNothingFound() {
            return isEmpty() && total > 0 && filter.isActive();
        }

        public List<ProductCategory> getCategories() {
            return List.of(ProductCategory.values());
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
        return load(ProductFilter.NONE);
    }

    /**
     * O catálogo de uma escola tem poucas dezenas de itens: a consulta traz os produtos ativos da escola
     * e o filtro roda em memória, o que permite buscar sem acento ("oculos" acha "Óculos").
     */
    @Transactional(readOnly = true)
    public Catalog load(ProductFilter filter) {
        UUID tenantId = tenantContext.getTenantId();

        List<Product> all = productRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId);
        List<Product> found = all.stream().filter(filter::matches).toList();
        Map<ProductCategory, List<Product>> byCategory = found.stream()
                .collect(Collectors.groupingBy(Product::getCategory,
                        () -> new EnumMap<>(ProductCategory.class), Collectors.toList()));

        List<PlanDiscount> discounts = planRepository.findByTenantIdAndActiveTrueOrderByDisplayOrderAsc(tenantId).stream()
                .filter(Plan::hasProductDiscount)
                .map(p -> new PlanDiscount(p.getName(), p.getProductDiscountPercent()))
                .toList();

        return new Catalog(byCategory, discounts, filter, found.size(), all.size());
    }
}

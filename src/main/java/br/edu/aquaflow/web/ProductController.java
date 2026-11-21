package br.edu.aquaflow.web;

import br.edu.aquaflow.service.site.ProductCatalogService;
import br.edu.aquaflow.service.site.ProductFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    /** Filtros na query string para o link poder ser compartilhado: /loja?q=oculos&categoria=treino&min=20&max=80 */
    @GetMapping("/loja")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String categoria,
                        @RequestParam(required = false) String min,
                        @RequestParam(required = false) String max,
                        Model model) {
        model.addAttribute("catalog", productCatalogService.load(ProductFilter.of(q, categoria, min, max)));
        return "products";
    }
}

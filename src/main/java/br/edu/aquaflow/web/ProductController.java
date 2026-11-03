package br.edu.aquaflow.web;

import br.edu.aquaflow.service.site.ProductCatalogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @GetMapping("/loja")
    public String index(Model model) {
        model.addAttribute("catalog", productCatalogService.load());
        return "products";
    }
}

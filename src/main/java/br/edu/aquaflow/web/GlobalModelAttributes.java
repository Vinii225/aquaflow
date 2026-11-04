package br.edu.aquaflow.web;

import br.edu.aquaflow.config.AquaflowProperties;
import br.edu.aquaflow.service.store.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Atributos disponíveis em todos os templates (dados da escola e a rota atual). */
@ControllerAdvice
public class GlobalModelAttributes {

    private final AquaflowProperties.School school;
    private final StoreService storeService;

    public GlobalModelAttributes(AquaflowProperties properties, StoreService storeService) {
        this.school = properties.school();
        this.storeService = storeService;
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return storeService.cartCount();
    }

    @ModelAttribute("schoolName")
    public String schoolName() {
        return school.name();
    }

    @ModelAttribute("schoolAddress")
    public String schoolAddress() {
        return school.address();
    }

    @ModelAttribute("schoolPhone")
    public String schoolPhone() {
        return school.phone();
    }

    @ModelAttribute("schoolPhoneHref")
    public String schoolPhoneHref() {
        return school.phoneHref();
    }

    // Thymeleaf 3.1 nao expoe mais o request; usado para marcar o item ativo no menu
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}

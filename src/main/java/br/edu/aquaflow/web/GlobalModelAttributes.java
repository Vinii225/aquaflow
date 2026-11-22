package br.edu.aquaflow.web;

import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.service.TenantContext;
import br.edu.aquaflow.service.store.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Atributos disponíveis em todos os templates (dados da escola atual e a rota atual). */
@ControllerAdvice
public class GlobalModelAttributes {

    private final TenantContext tenantContext;
    private final StoreService storeService;

    public GlobalModelAttributes(TenantContext tenantContext, StoreService storeService) {
        this.tenantContext = tenantContext;
        this.storeService = storeService;
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        return storeService.cartCount();
    }

    /** Nome, endereço e telefone vêm da escola da requisição ({@link TenantContext}). */
    @ModelAttribute
    public void school(Model model) {
        Tenant school = tenantContext.getTenant();
        model.addAttribute("schoolName", school.getName());
        model.addAttribute("schoolAddress", school.getAddress());
        model.addAttribute("schoolPhone", school.getPhone());
        model.addAttribute("schoolPhoneHref", school.getPhoneHref());
    }

    // Thymeleaf 3.1 nao expoe mais o request; usado para marcar o item ativo no menu
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}

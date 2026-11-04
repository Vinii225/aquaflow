package br.edu.aquaflow.web;

import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.store.StoreService;
import br.edu.aquaflow.web.dto.StoreCheckoutForm;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Carrinho, checkout e pedidos da loja (PB-04). Checkout e pedidos exigem aluno ({@code SecurityConfig}). */
@Controller
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/loja/carrinho")
    public String cart(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        model.addAttribute("cart", storeService.view(studentId(currentUser)));
        return "store/cart";
    }

    @PostMapping("/loja/carrinho")
    public String add(@RequestParam UUID productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false) String productName,
                      RedirectAttributes redirectAttributes) {
        if (storeService.addToCart(productId, quantity)) {
            redirectAttributes.addFlashAttribute("addedProductId", productId);
            redirectAttributes.addFlashAttribute("addedProductName", productName != null ? productName : "Produto");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Este produto não está mais disponível.");
        }
        return "redirect:/loja#produto-" + productId;
    }

    @PostMapping("/loja/carrinho/{productId}/quantidade")
    public String updateQuantity(@PathVariable UUID productId, @RequestParam int quantity) {
        storeService.updateQuantity(productId, quantity);
        return "redirect:/loja/carrinho";
    }

    @PostMapping("/loja/carrinho/{productId}/remover")
    public String remove(@PathVariable UUID productId) {
        storeService.removeFromCart(productId);
        return "redirect:/loja/carrinho";
    }

    @GetMapping("/loja/checkout")
    public String checkout(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        model.addAttribute("checkoutForm", new StoreCheckoutForm());
        return checkoutPage(currentUser, model);
    }

    @PostMapping("/loja/checkout")
    public String placeOrder(@Valid @ModelAttribute("checkoutForm") StoreCheckoutForm form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal AquaflowUserDetails currentUser,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            return checkoutPage(currentUser, model);
        }
        try {
            UUID orderId = storeService.checkout(currentUser.getUserId(), form.getPaymentMethod());
            return "redirect:/loja/pedidos/" + orderId;
        } catch (StoreService.EmptyCartException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/loja/carrinho";
        }
    }

    @GetMapping("/loja/pedidos/{orderId}")
    public String order(@PathVariable UUID orderId,
                        @AuthenticationPrincipal AquaflowUserDetails currentUser,
                        Model model) {
        model.addAttribute("order", storeService.findOrder(orderId, currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        return "store/order";
    }

    @GetMapping("/student/orders")
    public String myOrders(@AuthenticationPrincipal AquaflowUserDetails currentUser, Model model) {
        model.addAttribute("orders", storeService.ordersOf(currentUser.getUserId()));
        return "student/orders";
    }

    private String checkoutPage(AquaflowUserDetails currentUser, Model model) {
        var cart = storeService.view(currentUser.getUserId());
        if (cart.isEmpty()) {
            return "redirect:/loja/carrinho";
        }
        model.addAttribute("cart", cart);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        return "store/checkout";
    }

    private static UUID studentId(AquaflowUserDetails user) {
        return user != null && user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))
                ? user.getUserId() : null;
    }
}

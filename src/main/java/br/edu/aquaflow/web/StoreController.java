package br.edu.aquaflow.web;

import br.edu.aquaflow.domain.enums.PaymentMethod;
import br.edu.aquaflow.security.AquaflowUserDetails;
import br.edu.aquaflow.service.payment.PaymentGatewayException;
import br.edu.aquaflow.service.payment.PaymentService;
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
    private final PaymentService paymentService;
    private final PublicBaseUrl publicBaseUrl;

    public StoreController(StoreService storeService, PaymentService paymentService, PublicBaseUrl publicBaseUrl) {
        this.storeService = storeService;
        this.paymentService = paymentService;
        this.publicBaseUrl = publicBaseUrl;
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
                      @RequestParam(required = false) String q,
                      @RequestParam(required = false) String categoria,
                      @RequestParam(required = false) String min,
                      @RequestParam(required = false) String max,
                      RedirectAttributes redirectAttributes) {
        if (storeService.addToCart(productId, quantity)) {
            redirectAttributes.addFlashAttribute("addedProductId", productId);
            redirectAttributes.addFlashAttribute("addedProductName", productName != null ? productName : "Produto");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Este produto não está mais disponível.");
        }
        // Volta para a loja com a mesma busca e os mesmos filtros (o Spring codifica os valores na URL)
        keepFilter(redirectAttributes, "q", q);
        keepFilter(redirectAttributes, "categoria", categoria);
        keepFilter(redirectAttributes, "min", min);
        keepFilter(redirectAttributes, "max", max);
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
            var result = storeService.checkout(currentUser.getUserId(), form.getPaymentMethod(), publicBaseUrl.resolve());
            if (result.paymentRedirectUrl() != null) {
                return "redirect:" + result.paymentRedirectUrl();
            }
            return "redirect:/loja/pedidos/" + result.orderId();
        } catch (StoreService.EmptyCartException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/loja/carrinho";
        } catch (PaymentGatewayException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Não foi possível iniciar o pagamento agora. Nada foi cobrado; tente de novo em instantes.");
            return "redirect:/loja/checkout";
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
        model.addAttribute("onlinePayment", paymentService.isExternalGateway());
        return "store/checkout";
    }

    private static void keepFilter(RedirectAttributes redirectAttributes, String name, String value) {
        if (value != null && !value.isBlank()) {
            redirectAttributes.addAttribute(name, value.trim());
        }
    }

    private static UUID studentId(AquaflowUserDetails user) {
        return user != null && user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"))
                ? user.getUserId() : null;
    }
}

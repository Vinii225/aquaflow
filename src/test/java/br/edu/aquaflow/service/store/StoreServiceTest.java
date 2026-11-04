package br.edu.aquaflow.service.store;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StoreServiceTest {

    @Test
    void descontoArredondaParaOCentavo() {
        assertThat(StoreService.discountOf(new BigDecimal("99.80"), 15)).isEqualByComparingTo("14.97");
        assertThat(StoreService.discountOf(new BigDecimal("34.90"), 10)).isEqualByComparingTo("3.49");
    }

    @Test
    void semPlanoNaoTemDesconto() {
        assertThat(StoreService.discountOf(new BigDecimal("49.90"), 0)).isEqualByComparingTo("0");
    }

    @Test
    void carrinhoLimitaQuantidadeERemoveQuandoZera() {
        ShoppingCart cart = new ShoppingCart();
        UUID oculos = UUID.randomUUID();

        cart.add(oculos, 8);
        cart.add(oculos, 5);
        assertThat(cart.items()).containsEntry(oculos, ShoppingCart.MAX_QUANTITY);

        cart.setQuantity(oculos, 0);
        assertThat(cart.count()).isZero();
    }
}

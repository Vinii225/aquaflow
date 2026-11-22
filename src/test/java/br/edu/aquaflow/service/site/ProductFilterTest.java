package br.edu.aquaflow.service.site;

import br.edu.aquaflow.domain.Product;
import br.edu.aquaflow.domain.enums.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductFilterTest {

    private final Product oculos = product("Óculos de natação", ProductCategory.ACCESSORY, "49.90");
    private final Product prancha = product("Prancha de EVA", ProductCategory.TRAINING, "79.90");

    @Test
    void buscaIgnoraMaiusculasEAcentos() {
        assertThat(ProductFilter.of("OCULOS", null, null, null).matches(oculos)).isTrue();
        assertThat(ProductFilter.of("óculos", null, null, null).matches(oculos)).isTrue();
        assertThat(ProductFilter.of("natacao", null, null, null).matches(oculos)).isTrue();
        assertThat(ProductFilter.of("oculos", null, null, null).matches(prancha)).isFalse();
    }

    @Test
    void filtraPorCategoria() {
        ProductFilter treino = ProductFilter.of(null, "treino", null, null);
        assertThat(treino.matches(prancha)).isTrue();
        assertThat(treino.matches(oculos)).isFalse();
    }

    @Test
    void filtraPorFaixaDePrecoComLimitesInclusos() {
        assertThat(ProductFilter.of(null, null, "49,90", "60").matches(oculos)).isTrue();
        assertThat(ProductFilter.of(null, null, "50", null).matches(oculos)).isFalse();
        assertThat(ProductFilter.of(null, null, null, "79.90").matches(prancha)).isTrue();
        assertThat(ProductFilter.of(null, null, null, "79.89").matches(prancha)).isFalse();
    }

    @Test
    void combinaBuscaCategoriaEPreco() {
        ProductFilter f = ProductFilter.of("prancha", "treino", "70", "80");
        assertThat(f.matches(prancha)).isTrue();
        assertThat(ProductFilter.of("prancha", "acessorios", null, null).matches(prancha)).isFalse();
        assertThat(ProductFilter.of("prancha", "treino", "80", null).matches(prancha)).isFalse();
    }

    @Test
    void valoresInvalidosSaoIgnoradosEFaixaInvertidaECorrigida() {
        ProductFilter f = ProductFilter.of("  ", "inexistente", "abc", "-5");
        assertThat(f.isActive()).isFalse();

        ProductFilter invertido = ProductFilter.of(null, null, "100", "10");
        assertThat(invertido.min()).isEqualByComparingTo("10");
        assertThat(invertido.max()).isEqualByComparingTo("100");
    }

    private static Product product(String name, ProductCategory category, String price) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(new BigDecimal(price));
        return p;
    }
}

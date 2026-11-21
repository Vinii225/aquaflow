package br.edu.aquaflow.service.site;

import br.edu.aquaflow.domain.Product;
import br.edu.aquaflow.domain.enums.ProductCategory;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Busca e filtros da loja (PB-03), lidos da query string: {@code /loja?q=oculos&categoria=treino&min=20&max=80}.
 * Valores inválidos são ignorados em vez de dar erro, para um link compartilhado nunca quebrar a página.
 *
 * @param q         trecho do nome do produto (sem diferenciar maiúsculas, minúsculas nem acentos)
 * @param category  categoria, ou {@code null} para todas
 * @param min       preço mínimo, ou {@code null}
 * @param max       preço máximo, ou {@code null}
 */
public record ProductFilter(String q, ProductCategory category, BigDecimal min, BigDecimal max) {

    public static final ProductFilter NONE = new ProductFilter(null, null, null, null);

    /** Monta o filtro a partir dos parâmetros crus da URL. Faixa invertida (min > max) é corrigida. */
    public static ProductFilter of(String q, String categorySlug, String min, String max) {
        BigDecimal from = price(min);
        BigDecimal to = price(max);
        if (from != null && to != null && from.compareTo(to) > 0) {
            BigDecimal swap = from;
            from = to;
            to = swap;
        }
        String term = q == null || q.isBlank() ? null : q.trim();
        return new ProductFilter(term, ProductCategory.fromSlug(categorySlug).orElse(null), from, to);
    }

    public boolean isActive() {
        return q != null || category != null || min != null || max != null;
    }

    public String getCategorySlug() {
        return category == null ? null : category.getSlug();
    }

    public boolean matches(Product product) {
        if (category != null && product.getCategory() != category) {
            return false;
        }
        if (min != null && product.getPrice().compareTo(min) < 0) {
            return false;
        }
        if (max != null && product.getPrice().compareTo(max) > 0) {
            return false;
        }
        return q == null || normalize(product.getName()).contains(normalize(q));
    }

    /** "Óculos" e "oculos" ficam iguais. */
    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Aceita "49.90" e "49,90". Negativo ou texto inválido vira "sem limite". */
    static BigDecimal price(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
            return value.signum() < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

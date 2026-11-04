package br.edu.aquaflow.service.store;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Carrinho na sessão: funciona para visitante e sobrevive ao login. Guarda só id e quantidade. */
@Component
@SessionScope
public class ShoppingCart implements Serializable {

    public static final int MAX_QUANTITY = 10;

    private final Map<UUID, Integer> items = new LinkedHashMap<>();

    public synchronized void add(UUID productId, int quantity) {
        setQuantity(productId, items.getOrDefault(productId, 0) + quantity);
    }

    public synchronized void setQuantity(UUID productId, int quantity) {
        if (quantity <= 0) {
            items.remove(productId);
        } else {
            items.put(productId, Math.min(quantity, MAX_QUANTITY));
        }
    }

    public synchronized void remove(UUID productId) {
        items.remove(productId);
    }

    public synchronized void clear() {
        items.clear();
    }

    public synchronized Map<UUID, Integer> items() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(items));
    }

    public synchronized int count() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }
}

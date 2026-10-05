package br.edu.aquaflow.service;

import br.edu.aquaflow.domain.Tenant;
import br.edu.aquaflow.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantContext {

    private final TenantRepository tenantRepository;
    private volatile UUID cachedTenantId;

    public TenantContext(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant getTenant() {
        return tenantRepository.findById(getTenantId())
                .orElseThrow(() -> new IllegalStateException("Nenhuma escola (tenant) cadastrada"));
    }

    public UUID getTenantId() {
        if (cachedTenantId == null) {
            cachedTenantId = tenantRepository.findAll().stream()
                    .findFirst()
                    .map(Tenant::getId)
                    .orElseThrow(() -> new IllegalStateException("Nenhuma escola (tenant) cadastrada"));
        }
        return cachedTenantId;
    }
}

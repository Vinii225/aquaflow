package br.edu.aquaflow.domain;

import br.edu.aquaflow.domain.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String document;

    /** Identificador curto e único da escola (ex.: "aquaflow"), para URLs. */
    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    private String address;

    @Column(length = 32)
    private String phone;

    /** Escola usada para visitantes anônimos e usuários sem vínculo. */
    @Column(name = "default_school", nullable = false)
    private boolean defaultSchool;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Link clicável no celular: "(83) 99999-0000" -> "tel:+5583999990000". */
    public String getPhoneHref() {
        return phoneHref(phone);
    }

    static String phoneHref(String phone) {
        return phone == null || phone.isBlank() ? null : "tel:+55" + phone.replaceAll("\\D", "");
    }
}

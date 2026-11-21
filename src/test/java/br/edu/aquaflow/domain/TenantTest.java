package br.edu.aquaflow.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void telefoneViraLinkClicavel() {
        assertThat(Tenant.phoneHref("(83) 99999-0000")).isEqualTo("tel:+5583999990000");
    }

    @Test
    void semTelefoneNaoGeraLink() {
        assertThat(Tenant.phoneHref(null)).isNull();
        assertThat(Tenant.phoneHref("  ")).isNull();
    }
}

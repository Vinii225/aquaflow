package br.edu.aquaflow.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeekdayTest {

    @Test
    void numeracaoSegueOBancoDeUmASete() {
        assertThat(Weekday.of(1)).isEqualTo(Weekday.MONDAY);
        assertThat(Weekday.of(7).getLabel()).isEqualTo("Domingo");
        assertThat(Weekday.TUESDAY.number()).isEqualTo(2);
    }

    @Test
    void listaDeNomesTemPosicaoZeroVazia() {
        assertThat(Weekday.labelsByNumber()).hasSize(8).startsWith("", "Segunda");
    }

    @Test
    void rejeitaNumeroForaDaSemana() {
        assertThatThrownBy(() -> Weekday.of(0)).isInstanceOf(IllegalArgumentException.class);
    }
}

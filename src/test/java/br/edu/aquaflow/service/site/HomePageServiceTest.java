package br.edu.aquaflow.service.site;

import br.edu.aquaflow.domain.ClassSchedule;
import br.edu.aquaflow.service.site.HomePageService.Slot;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HomePageServiceTest {

    @Test
    void agrupaDiasComOMesmoHorario() {
        List<Slot> slots = HomePageService.groupSlots(List.of(
                schedule(2, "07:00", "08:00"),
                schedule(4, "07:00", "08:00"),
                schedule(6, "09:00", "10:00")));

        assertThat(slots).containsExactly(
                new Slot("Terça e Quinta", "07:00 – 08:00"),
                new Slot("Sábado", "09:00 – 10:00"));
    }

    @Test
    void juntaListasNoPadraoDoPortugues() {
        assertThat(HomePageService.joinWithAnd(List.of("Segunda"))).isEqualTo("Segunda");
        assertThat(HomePageService.joinWithAnd(List.of("Segunda", "Quarta", "Sexta"))).isEqualTo("Segunda, Quarta e Sexta");
    }

    @Test
    void iniciaisUsamPrimeiroEUltimoNome() {
        assertThat(HomePageService.initials("Ricardo Lima")).isEqualTo("RL");
        assertThat(HomePageService.initials("Ana Maria de Souza")).isEqualTo("AS");
        assertThat(HomePageService.initials("Igor")).isEqualTo("I");
    }

    private static ClassSchedule schedule(int day, String start, String end) {
        var s = new ClassSchedule();
        s.setDayOfWeek((short) day);
        s.setStartTime(LocalTime.parse(start));
        s.setEndTime(LocalTime.parse(end));
        return s;
    }
}

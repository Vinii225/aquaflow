package br.edu.aquaflow.service.dashboard;

import br.edu.aquaflow.domain.ClassSession;
import br.edu.aquaflow.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Test
    void receitaSeparaMensalidadeDeLojaEPreencheMesesVazios() {
        var chart = DashboardService.revenueByMonth(List.of(
                income("tuition", "150.00", LocalDateTime.of(2026, 11, 3, 10, 0)),
                income("store", "49.90", LocalDateTime.of(2026, 11, 4, 9, 0)),
                income("tuition", "99.90", LocalDateTime.of(2026, 9, 10, 8, 0))
        ), YearMonth.of(2026, 11), SP);

        assertThat(chart.months()).hasSize(6);
        assertThat(chart.months().get(0).month()).isEqualTo(YearMonth.of(2026, 6));
        var nov = chart.months().get(5);
        assertThat(nov.label()).isEqualTo("nov");
        assertThat(nov.tuition()).isEqualByComparingTo("150.00");
        assertThat(nov.store()).isEqualByComparingTo("49.90");
        assertThat(chart.months().get(4).getTotal()).isEqualByComparingTo("0");
        assertThat(chart.max()).isEqualByComparingTo("199.90");
        assertThat(chart.heightOf(nov.getTotal())).isEqualTo(100);
        assertThat(chart.heightOf(new BigDecimal("99.90"))).isEqualTo(50);
    }

    @Test
    void pagamentoNaViradaDoMesUsaOFusoDaEscola() {
        // 01/12 01:00 UTC ainda é 30/11 em São Paulo
        Transaction t = income("tuition", "100.00", LocalDateTime.of(2026, 11, 30, 22, 0));
        var chart = DashboardService.revenueByMonth(List.of(t), YearMonth.of(2026, 12), SP);
        assertThat(chart.months().get(4).tuition()).isEqualByComparingTo("100.00");
    }

    @Test
    void ocupacaoNaoPassaDe100PorAula() {
        var lotada = new DashboardService.SessionLoad(new ClassSession(), 12, 10);
        var vazia = new DashboardService.SessionLoad(new ClassSession(), 0, 10);
        assertThat(lotada.getPercent()).isEqualTo(100);
        assertThat(lotada.isFull()).isTrue();
        assertThat(DashboardService.occupancy(List.of(lotada, vazia))).isEqualTo(50);
        assertThat(DashboardService.occupancy(List.of())).isZero();
    }

    private static Transaction income(String category, String amount, LocalDateTime localTime) {
        Transaction t = new Transaction();
        t.setCategory(category);
        t.setAmount(new BigDecimal(amount));
        t.setPaidAt(localTime.atZone(SP).toInstant());
        return t;
    }
}

package br.edu.aquaflow.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PlanEnrollmentServiceTest {

    @Test
    void proximoVencimentoEhNoMesSeguinteADaContratacao() {
        assertThat(PlanEnrollmentService.nextDueDate(LocalDate.of(2026, 11, 4), 10))
                .isEqualTo(LocalDate.of(2026, 12, 10));
    }

    @Test
    void proximoVencimentoViraOAno() {
        assertThat(PlanEnrollmentService.nextDueDate(LocalDate.of(2026, 12, 28), 5))
                .isEqualTo(LocalDate.of(2027, 1, 5));
    }

    @Test
    void diaMaiorQueOMesCaiNoUltimoDia() {
        assertThat(PlanEnrollmentService.nextDueDate(LocalDate.of(2027, 1, 15), 31))
                .isEqualTo(LocalDate.of(2027, 2, 28));
    }
}

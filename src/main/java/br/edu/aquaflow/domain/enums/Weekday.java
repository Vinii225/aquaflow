package br.edu.aquaflow.domain.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Dia da semana como gravado em {@code class_schedules.day_of_week} (1 = segunda ... 7 = domingo, ISO-8601).
 * Fonte única dos nomes exibidos nas telas.
 */
public enum Weekday {
    MONDAY("Segunda"),
    TUESDAY("Terça"),
    WEDNESDAY("Quarta"),
    THURSDAY("Quinta"),
    FRIDAY("Sexta"),
    SATURDAY("Sábado"),
    SUNDAY("Domingo");

    private final String label;

    Weekday(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int number() {
        return ordinal() + 1;
    }

    public static Weekday of(int number) {
        if (number < 1 || number > 7) {
            throw new IllegalArgumentException("Dia da semana inválido: " + number);
        }
        return values()[number - 1];
    }

    /** Nomes indexados pelo número do dia (posição 0 vazia), no formato que os templates já usam. */
    public static List<String> labelsByNumber() {
        return Stream.concat(Stream.of(""), Arrays.stream(values()).map(Weekday::getLabel)).toList();
    }
}

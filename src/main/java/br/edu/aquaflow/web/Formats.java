package br.edu.aquaflow.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação em pt-BR para os templates, independente do locale do servidor.
 * Uso: {@code ${@formats.brl(valor)}}.
 */
@Component("formats")
public class Formats {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd/MM", PT_BR);
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR);

    /** R$ 1.234,50 */
    public String brl(BigDecimal value) {
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return "R$ " + f.format(value == null ? BigDecimal.ZERO : value);
    }

    /** Rótulo curto para cima das barras: 980, 1,2 mil, 15 mil. */
    public String brlShort(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        if (value.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
        }
        BigDecimal thousands = value.divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP).stripTrailingZeros();
        return NumberFormat.getNumberInstance(PT_BR).format(thousands) + " mil";
    }

    /** ter 04/11 */
    public String day(LocalDate date) {
        return DAY.format(date).replace(".", "");
    }

    /** novembro de 2026 */
    public String monthYear(LocalDate date) {
        return MONTH_YEAR.format(date);
    }
}

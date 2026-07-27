package com.vendafacil.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Valores monetários em Real.
 *
 * <p>Dinheiro é sempre representado como {@code long} de centavos — nunca
 * {@code double} — para que somas e multiplicações sejam exatas.
 */
public final class Moeda {

    /** Teto de R$ 999.999.999,99: acima disso o cálculo de totais fica arriscado. */
    public static final long MAX_CENTAVOS = 999_999_999_99L;

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Moeda() {}

    /** Formata centavos como {@code "R$ 1.234,56"}. */
    public static String formatar(long centavos) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(PT_BR);
        return nf.format(BigDecimal.valueOf(centavos, 2));
    }

    /** Formata centavos como {@code "1234,56"}, para preencher campos de edição. */
    public static String paraCampo(long centavos) {
        return String.format(Locale.ROOT, "%d,%02d", centavos / 100, Math.abs(centavos % 100));
    }

    /**
     * Converte texto digitado pelo usuário em centavos.
     * Aceita {@code "10,50"}, {@code "10.50"}, {@code "1.234,56"}, {@code "R$ 5"}…
     *
     * @throws RegraDeNegocioException se o texto não for um valor válido.
     */
    public static long parseCentavos(String texto) {
        if (texto == null)
            throw new RegraDeNegocioException("Informe um valor.");
        String t = texto.replace("R$", "").replace("r$", "").replace(" ", "").trim();
        if (t.isEmpty())
            throw new RegraDeNegocioException("Informe um valor.");

        boolean temVirgula = t.indexOf(',') >= 0;
        boolean temPonto = t.indexOf('.') >= 0;
        if (temVirgula && temPonto) {
            // Formato brasileiro completo: ponto de milhar, vírgula decimal.
            t = t.replace(".", "").replace(',', '.');
        } else if (temVirgula) {
            t = t.replace(',', '.');
        } else if (temPonto) {
            // Um único ponto seguido de 3 dígitos é separador de milhar ("1.234").
            int i = t.indexOf('.');
            if (i == t.lastIndexOf('.') && t.length() - i - 1 == 3) {
                t = t.replace(".", "");
            }
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("Valor inválido: \"" + texto.trim() + "\".");
        }
        if (valor.signum() < 0)
            throw new RegraDeNegocioException("O valor não pode ser negativo.");

        long centavos;
        try {
            centavos = valor.movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (ArithmeticException e) {
            throw new RegraDeNegocioException("Valor muito alto.");
        }
        if (centavos > MAX_CENTAVOS)
            throw new RegraDeNegocioException("Valor muito alto.");
        return centavos;
    }
}

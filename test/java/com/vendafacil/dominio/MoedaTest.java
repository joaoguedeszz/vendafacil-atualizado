package com.vendafacil.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoedaTest {

    @ParameterizedTest(name = "\"{0}\" → {1} centavos")
    @CsvSource({
            "'10,50',      1050",
            "'10.50',      1050",
            "'1.234,56',   123456",
            "'R$ 5',       500",
            "'r$ 12,90',   1290",
            "'  7 ',       700",
            "'0',          0",
            "'0,01',       1",
            "'1.234',      123400",   // ponto de milhar, não decimal
            "'1.23',       123",      // ponto decimal (2 casas)
            "'10,999',     1100",     // arredonda para cima
            "'10,994',     1099",
    })
    @DisplayName("aceita os formatos que o usuário costuma digitar")
    void interpretaFormatosBrasileiros(String entrada, long centavosEsperados) {
        assertEquals(centavosEsperados, Moeda.parseCentavos(entrada));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "10,5,3", "R$", "-", "12,,5"})
    @DisplayName("recusa texto que não é um valor")
    void recusaEntradaInvalida(String entrada) {
        assertThrows(RegraDeNegocioException.class, () -> Moeda.parseCentavos(entrada));
    }

    @Test
    @DisplayName("recusa valor nulo, negativo ou acima do teto")
    void recusaValoresForaDaFaixa() {
        assertThrows(RegraDeNegocioException.class, () -> Moeda.parseCentavos(null));
        assertThrows(RegraDeNegocioException.class, () -> Moeda.parseCentavos("-1"));
        assertThrows(RegraDeNegocioException.class,
                () -> Moeda.parseCentavos("999999999999999999999"));
    }

    @Test
    @DisplayName("aceita exatamente o teto e recusa um centavo acima")
    void respeitaOTeto() {
        assertEquals(Moeda.MAX_CENTAVOS, Moeda.parseCentavos("999999999,99"));
        assertThrows(RegraDeNegocioException.class,
                () -> Moeda.parseCentavos("1000000000,00"));
    }

    @Test
    @DisplayName("formata em Real com separadores brasileiros")
    void formata() {
        // Só o número é verificado: o separador entre "R$" e o valor varia
        // conforme a versão do JDK (espaço comum ou não separável).
        assertTrue(Moeda.formatar(123456).endsWith("1.234,56"),
                () -> "esperado terminar em 1.234,56, veio " + Moeda.formatar(123456));
        assertTrue(Moeda.formatar(0).endsWith("0,00"));
        assertTrue(Moeda.formatar(123456).startsWith("R$"));
    }

    @Test
    @DisplayName("paraCampo devolve o valor pronto para reedição")
    void formataParaCampo() {
        assertEquals("12,50", Moeda.paraCampo(1250));
        assertEquals("0,05", Moeda.paraCampo(5));
        assertEquals("1234,00", Moeda.paraCampo(123400));
    }

    @Test
    @DisplayName("formatar e reler devolve o mesmo valor")
    void idaEVolta() {
        for (long centavos : new long[]{0, 1, 99, 100, 1050, 123456, 99999999999L}) {
            assertEquals(centavos, Moeda.parseCentavos(Moeda.paraCampo(centavos)));
        }
    }
}

package com.vendafacil.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 7, 26, 14, 30);

    @Test
    @DisplayName("congela nome e preço do produto no momento da venda")
    void congelaDadosDoProduto() {
        Produto produto = Produto.novo("Café 500g", 1250, 10).comId(3);

        Venda venda = Venda.de(produto, 2, AGORA);

        assertEquals(3L, venda.produtoId());
        assertEquals("Café 500g", venda.nomeProduto());
        assertEquals(1250, venda.precoUnitarioCentavos());
        assertEquals(2, venda.quantidade());
        assertEquals(2500, venda.totalCentavos());
    }

    @Test
    @DisplayName("recusa venda sem quantidade, sem nome ou com preço negativo")
    void recusaDadosInvalidos() {
        assertThrows(RegraDeNegocioException.class,
                () -> new Venda(1, 1L, "Café", 0, 100, AGORA));
        assertThrows(RegraDeNegocioException.class,
                () -> new Venda(1, 1L, "Café", -1, 100, AGORA));
        assertThrows(RegraDeNegocioException.class,
                () -> new Venda(1, 1L, "  ", 1, 100, AGORA));
        assertThrows(RegraDeNegocioException.class,
                () -> new Venda(1, 1L, "Café", 1, -1, AGORA));
        assertThrows(RegraDeNegocioException.class,
                () -> new Venda(1, 1L, "Café", 1, 100, null));
    }

    @Test
    @DisplayName("venda sem produto de origem continua válida")
    void aceitaVendaOrfa() {
        Venda venda = new Venda(1, null, "Produto excluído", 2, 500, AGORA);

        assertTrue(venda.produtoDeOrigem().isEmpty());
        assertEquals(1000, venda.totalCentavos());
    }
}

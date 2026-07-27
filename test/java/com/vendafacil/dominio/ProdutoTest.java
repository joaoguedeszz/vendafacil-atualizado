package com.vendafacil.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoTest {

    @Test
    @DisplayName("apara o nome ao construir")
    void aparaNome() {
        assertEquals("Café 500g", Produto.novo("  Café 500g  ", 1250, 3).nome());
    }

    @Test
    @DisplayName("não existe produto inválido em memória")
    void recusaDadosInvalidos() {
        assertThrows(RegraDeNegocioException.class, () -> Produto.novo(null, 100, 1));
        assertThrows(RegraDeNegocioException.class, () -> Produto.novo("   ", 100, 1));
        assertThrows(RegraDeNegocioException.class, () -> Produto.novo("Café", -1, 1));
        assertThrows(RegraDeNegocioException.class, () -> Produto.novo("Café", 100, -1));
        assertThrows(RegraDeNegocioException.class,
                () -> Produto.novo("Café", Moeda.MAX_CENTAVOS + 1, 1));
        assertThrows(RegraDeNegocioException.class,
                () -> Produto.novo("x".repeat(Produto.NOME_MAX + 1), 100, 1));
        assertThrows(RegraDeNegocioException.class,
                () -> Produto.novo("Café", 100, Produto.QUANTIDADE_MAX + 1));
    }

    @Test
    @DisplayName("baixar estoque devolve um novo produto e nunca fica negativo")
    void baixaEstoque() {
        Produto original = Produto.novo("Café", 1000, 5);

        Produto baixado = original.baixarEstoque(2);

        assertEquals(3, baixado.quantidade());
        assertEquals(5, original.quantidade(), "o produto original não pode mudar");
        assertThrows(RegraDeNegocioException.class, () -> original.baixarEstoque(6));
        assertThrows(RegraDeNegocioException.class, () -> original.baixarEstoque(0));
        assertThrows(RegraDeNegocioException.class, () -> original.baixarEstoque(-1));
    }

    @Test
    @DisplayName("baixar exatamente todo o estoque é permitido")
    void baixaEstoqueCompleto() {
        assertEquals(0, Produto.novo("Café", 1000, 5).baixarEstoque(5).quantidade());
    }

    @Test
    @DisplayName("repor estoque soma e exige quantidade positiva")
    void repoeEstoque() {
        assertEquals(8, Produto.novo("Café", 1000, 5).reporEstoque(3).quantidade());
        assertThrows(RegraDeNegocioException.class,
                () -> Produto.novo("Café", 1000, 5).reporEstoque(0));
    }

    @Test
    @DisplayName("classifica a situação do estoque pelas faixas")
    void classificaSituacao() {
        assertEquals(SituacaoEstoque.ESGOTADO, Produto.novo("A", 1, 0).situacao());
        assertEquals(SituacaoEstoque.BAIXO, Produto.novo("B", 1, 1).situacao());
        assertEquals(SituacaoEstoque.BAIXO,
                Produto.novo("C", 1, SituacaoEstoque.LIMIAR_BAIXO).situacao());
        assertEquals(SituacaoEstoque.NORMAL,
                Produto.novo("D", 1, SituacaoEstoque.LIMIAR_BAIXO + 1).situacao());
    }

    @Test
    @DisplayName("normaliza o nome ignorando maiúsculas e acentuação de caixa")
    void normalizaNome() {
        assertEquals("café 500g", Produto.novo("CAFÉ 500g", 1, 1).nomeNormalizado());
        assertEquals(Produto.novo("Café", 1, 1).nomeNormalizado(),
                Produto.novo("CAFÉ", 1, 1).nomeNormalizado());
    }

    @Test
    @DisplayName("valor em estoque é preço vezes quantidade")
    void calculaValorEmEstoque() {
        assertEquals(3750, Produto.novo("Café", 1250, 3).valorEmEstoqueCentavos());
        assertEquals(0, Produto.novo("Café", 1250, 0).valorEmEstoqueCentavos());
    }

    @Test
    @DisplayName("sabe se já foi gravado")
    void sabeSeFoiPersistido() {
        assertFalse(Produto.novo("Café", 1, 1).persistido());
        assertTrue(Produto.novo("Café", 1, 1).comId(7).persistido());
    }
}

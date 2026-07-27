package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.dominio.SituacaoEstoque;
import com.vendafacil.suporte.BancoTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdutoServicoTest {

    private BancoTeste banco;
    private ProdutoServico produtos;

    @BeforeEach
    void abrir() {
        banco = new BancoTeste();
        produtos = new ProdutoServico(banco.produtos());
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    @Test
    @DisplayName("cadastra e recupera com o id atribuído pelo banco")
    void cadastraERecupera() {
        Produto salvo = produtos.cadastrar("Café 500g", 1250, 10);

        assertTrue(salvo.persistido());
        Produto lido = produtos.porId(salvo.id()).orElseThrow();
        assertEquals("Café 500g", lido.nome());
        assertEquals(1250, lido.precoCentavos());
        assertEquals(10, lido.quantidade());
    }

    @Test
    @DisplayName("produtos diferentes recebem ids diferentes")
    void geraIdsDistintos() {
        assertEquals(2, List.of(produtos.cadastrar("Café", 100, 1).id(),
                produtos.cadastrar("Chá", 100, 1).id()).stream().distinct().count());
    }

    @Test
    @DisplayName("recusa nome repetido, mesmo com caixa e espaços diferentes")
    void recusaNomeDuplicado() {
        produtos.cadastrar("Café 500g", 1250, 10);

        assertThrows(RegraDeNegocioException.class,
                () -> produtos.cadastrar("Café 500g", 999, 1));
        assertThrows(RegraDeNegocioException.class,
                () -> produtos.cadastrar("CAFÉ 500G", 999, 1));
        assertThrows(RegraDeNegocioException.class,
                () -> produtos.cadastrar("  café 500g  ", 999, 1));
        assertEquals(1, produtos.listar().size());
    }

    @Test
    @DisplayName("editar mantendo o próprio nome não colide consigo mesmo")
    void permiteManterOProprioNome() {
        Produto café = produtos.cadastrar("Café", 1000, 5);

        Produto atualizado = produtos.atualizar(café.id(), "Café", 1500, 8);

        assertEquals(1500, atualizado.precoCentavos());
        assertEquals(8, atualizado.quantidade());
        assertEquals(1500, produtos.porId(café.id()).orElseThrow().precoCentavos());
    }

    @Test
    @DisplayName("editar para o nome de outro produto é recusado")
    void recusaNomeDeOutroProduto() {
        produtos.cadastrar("Café", 1000, 5);
        Produto cha = produtos.cadastrar("Chá", 800, 5);

        assertThrows(RegraDeNegocioException.class,
                () -> produtos.atualizar(cha.id(), "Café", 800, 5));
    }

    @Test
    @DisplayName("operações sobre produto inexistente falham com mensagem de negócio")
    void recusaProdutoInexistente() {
        assertThrows(RegraDeNegocioException.class,
                () -> produtos.atualizar(999, "Café", 100, 1));
        assertThrows(RegraDeNegocioException.class, () -> produtos.excluir(999));
        assertTrue(produtos.porId(999).isEmpty());
    }

    @Test
    @DisplayName("exclui do cadastro")
    void exclui() {
        Produto café = produtos.cadastrar("Café", 1000, 5);

        produtos.excluir(café.id());

        assertTrue(produtos.listar().isEmpty());
    }

    @Test
    @DisplayName("busca por trecho do nome, ignorando maiúsculas")
    void busca() {
        produtos.cadastrar("Café 500g", 1250, 10);
        produtos.cadastrar("Café 1kg", 2200, 4);
        produtos.cadastrar("Chá verde", 800, 7);

        assertEquals(2, produtos.buscar("café").size());
        assertEquals(2, produtos.buscar("CAFÉ").size());
        assertEquals(1, produtos.buscar("verde").size());
        assertEquals(3, produtos.buscar("").size(), "busca vazia lista tudo");
        assertEquals(0, produtos.buscar("inexistente").size());
    }

    @Test
    @DisplayName("curingas do LIKE são tratados como texto comum")
    void buscaNaoInterpretaCuringas() {
        produtos.cadastrar("Desconto 50%", 100, 1);
        produtos.cadastrar("Caneta azul", 100, 1);

        assertEquals(1, produtos.buscar("50%").size());
        assertEquals(0, produtos.buscar("%%%").size());
        assertEquals(0, produtos.buscar("_").size());
    }

    @Test
    @DisplayName("lista em ordem alfabética")
    void listaOrdenado() {
        produtos.cadastrar("Chá", 100, 1);
        produtos.cadastrar("Açúcar", 100, 1);
        produtos.cadastrar("Bolacha", 100, 1);

        assertEquals(List.of("Açúcar", "Bolacha", "Chá"),
                produtos.listar().stream().map(Produto::nome).toList());
    }

    @Test
    @DisplayName("alerta de estoque traz os críticos primeiro")
    void listaEstoqueBaixo() {
        produtos.cadastrar("Cheio", 100, SituacaoEstoque.LIMIAR_BAIXO + 1);
        produtos.cadastrar("Baixo", 100, 3);
        produtos.cadastrar("Esgotado", 100, 0);
        produtos.cadastrar("No limite", 100, SituacaoEstoque.LIMIAR_BAIXO);

        assertEquals(List.of("Esgotado", "Baixo", "No limite"),
                produtos.estoqueBaixo().stream().map(Produto::nome).toList());
    }

    @Test
    @DisplayName("só produtos com estoque aparecem para venda")
    void listaDisponiveis() {
        produtos.cadastrar("Com estoque", 100, 2);
        produtos.cadastrar("Esgotado", 100, 0);

        assertEquals(List.of("Com estoque"),
                produtos.disponiveisParaVenda().stream().map(Produto::nome).toList());
    }
}

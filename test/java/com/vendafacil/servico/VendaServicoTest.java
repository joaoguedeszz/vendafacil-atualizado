package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.dominio.Venda;
import com.vendafacil.suporte.BancoTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaServicoTest {

    private BancoTeste banco;
    private ProdutoServico produtos;
    private VendaServico vendas;

    @BeforeEach
    void abrir() {
        banco = new BancoTeste();
        produtos = new ProdutoServico(banco.produtos());
        vendas = new VendaServico(banco.transacoes(), banco.produtos(), banco.vendas());
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    @Test
    @DisplayName("registrar baixa o estoque e grava a venda")
    void registraVenda() {
        Produto café = produtos.cadastrar("Café", 1250, 10);

        Venda venda = vendas.registrar(café.id(), 3);

        assertEquals(7, produtos.porId(café.id()).orElseThrow().quantidade());
        assertEquals(3, venda.quantidade());
        assertEquals(1250, venda.precoUnitarioCentavos());
        assertEquals(3750, venda.totalCentavos());
        assertEquals(1, vendas.listar().size());
    }

    @Test
    @DisplayName("estoque insuficiente não gera venda nem baixa parcial")
    void recusaEstoqueInsuficiente() {
        Produto café = produtos.cadastrar("Café", 1250, 2);

        assertThrows(RegraDeNegocioException.class, () -> vendas.registrar(café.id(), 3));

        // A transação foi desfeita por inteiro.
        assertEquals(2, produtos.porId(café.id()).orElseThrow().quantidade());
        assertTrue(vendas.listar().isEmpty());
        assertEquals(0, vendas.receitaTotalCentavos());
    }

    @Test
    @DisplayName("vender de produto inexistente falha sem efeito colateral")
    void recusaProdutoInexistente() {
        assertThrows(RegraDeNegocioException.class, () -> vendas.registrar(999, 1));
        assertTrue(vendas.listar().isEmpty());
    }

    @Test
    @DisplayName("quantidade zero ou negativa é recusada")
    void recusaQuantidadeInvalida() {
        Produto café = produtos.cadastrar("Café", 1250, 10);

        assertThrows(RegraDeNegocioException.class, () -> vendas.registrar(café.id(), 0));
        assertThrows(RegraDeNegocioException.class, () -> vendas.registrar(café.id(), -2));
        assertEquals(10, produtos.porId(café.id()).orElseThrow().quantidade());
    }

    @Test
    @DisplayName("cancelar devolve a quantidade ao estoque e apaga a venda")
    void cancelaVenda() {
        Produto café = produtos.cadastrar("Café", 1250, 10);
        Venda venda = vendas.registrar(café.id(), 4);

        boolean estoqueRestaurado = vendas.cancelar(venda.id());

        assertTrue(estoqueRestaurado);
        assertEquals(10, produtos.porId(café.id()).orElseThrow().quantidade());
        assertTrue(vendas.listar().isEmpty());
        assertEquals(0, vendas.receitaTotalCentavos());
    }

    @Test
    @DisplayName("cancelar venda inexistente é recusado")
    void recusaCancelamentoInexistente() {
        assertThrows(RegraDeNegocioException.class, () -> vendas.cancelar(999));
    }

    @Test
    @DisplayName("editar o produto depois não reescreve o histórico")
    void historicoNaoMudaAoEditarProduto() {
        Produto café = produtos.cadastrar("Café 500g", 1000, 10);
        vendas.registrar(café.id(), 2);

        produtos.atualizar(café.id(), "Café 500g premium", 5000, 10);

        Venda registrada = vendas.listar().get(0);
        assertEquals("Café 500g", registrada.nomeProduto());
        assertEquals(1000, registrada.precoUnitarioCentavos());
        assertEquals(2000, vendas.receitaTotalCentavos());
    }

    @Test
    @DisplayName("excluir o produto preserva a venda, apenas sem vínculo")
    void excluirProdutoPreservaHistorico() {
        Produto café = produtos.cadastrar("Café 500g", 1000, 10);
        vendas.registrar(café.id(), 2);

        produtos.excluir(café.id());

        Venda registrada = vendas.listar().get(0);
        assertEquals("Café 500g", registrada.nomeProduto());
        assertEquals(1000, registrada.precoUnitarioCentavos());
        assertTrue(registrada.produtoDeOrigem().isEmpty(),
                "o vínculo deve virar nulo, não apontar para um id inexistente");
        assertEquals(2000, vendas.receitaTotalCentavos());
    }

    @Test
    @DisplayName("cancelar venda de produto já excluído remove a venda e avisa")
    void cancelaVendaOrfa() {
        Produto café = produtos.cadastrar("Café", 1000, 10);
        Venda venda = vendas.registrar(café.id(), 2);
        produtos.excluir(café.id());

        boolean estoqueRestaurado = vendas.cancelar(venda.id());

        assertFalse(estoqueRestaurado, "não há estoque para restaurar");
        assertTrue(vendas.listar().isEmpty());
    }

    @Test
    @DisplayName("receita soma todas as vendas")
    void somaReceita() {
        Produto café = produtos.cadastrar("Café", 1250, 10);
        Produto cha = produtos.cadastrar("Chá", 800, 10);
        vendas.registrar(café.id(), 2);   // 2500
        vendas.registrar(cha.id(), 3);    // 2400

        assertEquals(4900, vendas.receitaTotalCentavos());
    }

    @Test
    @DisplayName("histórico vem da venda mais recente para a mais antiga")
    void ordenaHistorico() {
        Produto café = produtos.cadastrar("Café", 100, 10);
        Venda primeira = vendas.registrar(café.id(), 1);
        Venda segunda = vendas.registrar(café.id(), 1);
        Venda terceira = vendas.registrar(café.id(), 1);

        // Todas caem no mesmo segundo: o desempate por id garante a ordem.
        assertEquals(List.of(terceira.id(), segunda.id(), primeira.id()),
                vendas.listar().stream().map(Venda::id).toList());
        assertEquals(List.of(terceira.id(), segunda.id()),
                vendas.ultimas(2).stream().map(Venda::id).toList());
    }

    @Test
    @DisplayName("vender tudo deixa o produto esgotado, mas ainda cadastrado")
    void vendeEstoqueCompleto() {
        Produto café = produtos.cadastrar("Café", 100, 3);

        vendas.registrar(café.id(), 3);

        assertEquals(0, produtos.porId(café.id()).orElseThrow().quantidade());
        assertTrue(produtos.disponiveisParaVenda().isEmpty());
        assertEquals(1, produtos.listar().size());
    }
}

package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.dominio.Venda;
import com.vendafacil.repositorio.ProdutoRepositorio;
import com.vendafacil.repositorio.Transacoes;
import com.vendafacil.repositorio.VendaRepositorio;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Regras de venda.
 *
 * <p>Registrar e cancelar mexem em duas tabelas ao mesmo tempo, então rodam
 * dentro de uma transação: ou o estoque baixa <em>e</em> a venda é gravada, ou
 * nada acontece. Sem isso, uma falha no meio deixaria estoque e histórico
 * discordando um do outro.
 */
public class VendaServico {

    private final Transacoes transacoes;
    private final ProdutoRepositorio produtos;
    private final VendaRepositorio vendas;

    public VendaServico(Transacoes transacoes, ProdutoRepositorio produtos,
                        VendaRepositorio vendas) {
        this.transacoes = transacoes;
        this.produtos = produtos;
        this.vendas = vendas;
    }

    public List<Venda> listar() {
        return vendas.todas();
    }

    public List<Venda> ultimas(int limite) {
        return vendas.ultimas(limite);
    }

    public Optional<Venda> porId(long id) {
        return vendas.porId(id);
    }

    public long receitaTotalCentavos() {
        return vendas.receitaTotalCentavos();
    }

    /**
     * Registra a venda de um produto e baixa o estoque.
     *
     * <p>O preço unitário e o nome ficam congelados na venda: alterações
     * posteriores no cadastro não reescrevem o histórico.
     */
    public Venda registrar(long produtoId, int quantidade) {
        return transacoes.executar(() -> {
            Produto produto = produtos.porId(produtoId)
                    .orElseThrow(() -> new RegraDeNegocioException("Produto não encontrado."));
            // Valida quantidade e estoque disponível; lança se não couber.
            Produto baixado = produto.baixarEstoque(quantidade);
            produtos.atualizar(baixado);
            return vendas.inserir(Venda.de(produto, quantidade, agora()));
        });
    }

    /**
     * Cancela uma venda e devolve a quantidade ao estoque.
     *
     * @return true se o estoque foi restaurado; false se o produto de origem
     *         já não existe no cadastro (a venda é removida de todo jeito).
     */
    public boolean cancelar(long vendaId) {
        return transacoes.executar(() -> {
            Venda venda = vendas.porId(vendaId)
                    .orElseThrow(() -> new RegraDeNegocioException("Venda não encontrada."));
            Optional<Produto> origem = venda.produtoDeOrigem().flatMap(produtos::porId);
            origem.ifPresent(p -> produtos.atualizar(p.reporEstoque(venda.quantidade())));
            vendas.excluir(vendaId);
            return origem.isPresent();
        });
    }

    /** Segundos inteiros: é a precisão que o banco guarda. */
    private static LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}

package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.dominio.SituacaoEstoque;
import com.vendafacil.repositorio.ProdutoRepositorio;

import java.util.List;
import java.util.Optional;

/** Regras de cadastro de produtos. */
public class ProdutoServico {

    private final ProdutoRepositorio produtos;

    public ProdutoServico(ProdutoRepositorio produtos) {
        this.produtos = produtos;
    }

    public List<Produto> listar() {
        return produtos.todos();
    }

    public List<Produto> buscar(String termo) {
        return produtos.buscarPorNome(termo);
    }

    public Optional<Produto> porId(long id) {
        return produtos.porId(id);
    }

    /** Produtos que precisam de reposição, do mais crítico ao menos. */
    public List<Produto> estoqueBaixo() {
        return produtos.comEstoqueAte(SituacaoEstoque.LIMIAR_BAIXO);
    }

    /** Produtos com estoque para vender. */
    public List<Produto> disponiveisParaVenda() {
        return produtos.disponiveis();
    }

    public Produto cadastrar(String nome, long precoCentavos, int quantidade) {
        Produto novo = Produto.novo(nome, precoCentavos, quantidade);
        exigirNomeLivre(novo.nome(), Produto.SEM_ID);
        return produtos.inserir(novo);
    }

    public Produto atualizar(long id, String nome, long precoCentavos, int quantidade) {
        Produto atual = produtos.porId(id)
                .orElseThrow(() -> new RegraDeNegocioException("Produto não encontrado."));
        Produto alterado = atual.comDados(nome, precoCentavos, quantidade);
        exigirNomeLivre(alterado.nome(), id);
        if (!produtos.atualizar(alterado))
            throw new RegraDeNegocioException("Produto não encontrado.");
        return alterado;
    }

    /**
     * Exclui o produto. O histórico de vendas dele é preservado — a venda
     * guarda a própria cópia do nome e do preço.
     */
    public void excluir(long id) {
        if (!produtos.excluir(id))
            throw new RegraDeNegocioException("Produto não encontrado.");
    }

    private void exigirNomeLivre(String nome, long idAtual) {
        produtos.porNome(nome, idAtual).ifPresent(existente -> {
            throw new RegraDeNegocioException(
                    "Já existe um produto chamado \"" + existente.nome() + "\".");
        });
    }
}

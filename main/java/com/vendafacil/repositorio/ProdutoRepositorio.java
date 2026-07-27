package com.vendafacil.repositorio;

import com.vendafacil.dominio.Produto;

import java.util.List;
import java.util.Optional;

/**
 * Acesso aos produtos.
 *
 * <p>A interface existe para que os serviços dependam de um contrato e não do
 * SQLite: trocar o armazenamento (ou usar um dublê em teste) não toca nas
 * regras de negócio.
 *
 * <p>Contagens e somas são declaradas aqui — e não calculadas percorrendo
 * {@link #todos()} — para que o banco as resolva com um agregado.
 */
public interface ProdutoRepositorio {

    /** Grava um produto novo e devolve a cópia com o id atribuído. */
    Produto inserir(Produto novo);

    /** @return false se o produto não existe mais. */
    boolean atualizar(Produto produto);

    /** @return false se não havia nada para excluir. */
    boolean excluir(long id);

    Optional<Produto> porId(long id);

    /** Todos os produtos, em ordem alfabética. */
    List<Produto> todos();

    /** Produtos cujo nome contém o termo, sem diferenciar maiúsculas/acentos. */
    List<Produto> buscarPorNome(String termo);

    /** Produtos com quantidade menor ou igual ao limiar, do mais crítico ao menos. */
    List<Produto> comEstoqueAte(int limiar);

    /** Produtos com pelo menos uma unidade disponível. */
    List<Produto> disponiveis();

    /**
     * @param idIgnorado id a desconsiderar na checagem (o do próprio produto,
     *                   ao editar); use {@link Produto#SEM_ID} para nenhum.
     */
    Optional<Produto> porNome(String nome, long idIgnorado);

    int contar();

    long somaUnidades();

    long somaValorEstoqueCentavos();
}

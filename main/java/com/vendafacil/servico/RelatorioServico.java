package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.SituacaoEstoque;
import com.vendafacil.dominio.Venda;
import com.vendafacil.repositorio.ProdutoRepositorio;
import com.vendafacil.repositorio.VendaRepositorio;

import java.util.List;

/**
 * Números consolidados para o dashboard.
 *
 * <p>Somas e contagens são resolvidas pelo banco com agregados — antes, cada
 * indicador percorria a lista inteira de produtos e vendas em memória.
 */
public class RelatorioServico {

    private final ProdutoRepositorio produtos;
    private final VendaRepositorio vendas;

    public RelatorioServico(ProdutoRepositorio produtos, VendaRepositorio vendas) {
        this.produtos = produtos;
        this.vendas = vendas;
    }

    public Indicadores indicadores() {
        List<Produto> alertas = produtos.comEstoqueAte(SituacaoEstoque.LIMIAR_BAIXO);
        return new Indicadores(
                vendas.receitaTotalCentavos(),
                vendas.contar(),
                produtos.somaUnidades(),
                produtos.contar(),
                alertas.size(),
                produtos.somaValorEstoqueCentavos());
    }

    /** Produtos que precisam de reposição, do mais crítico ao menos. */
    public List<Produto> alertasEstoque() {
        return produtos.comEstoqueAte(SituacaoEstoque.LIMIAR_BAIXO);
    }

    public List<Venda> ultimasVendas(int limite) {
        return vendas.ultimas(limite);
    }
}

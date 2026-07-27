package com.vendafacil.servico;

/**
 * Números do dashboard, calculados de uma vez só.
 *
 * @param receitaTotalCentavos soma de todas as vendas do histórico
 * @param quantidadeVendas     vendas registradas
 * @param unidadesEmEstoque    soma das quantidades de todos os produtos
 * @param quantidadeProdutos   produtos cadastrados
 * @param alertasEstoque       produtos que precisam de reposição
 * @param valorEstoqueCentavos preço × quantidade somado sobre o estoque
 */
public record Indicadores(long receitaTotalCentavos, int quantidadeVendas,
                          long unidadesEmEstoque, int quantidadeProdutos,
                          int alertasEstoque, long valorEstoqueCentavos) {
}

package com.vendafacil.dominio;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Registro imutável de uma venda.
 *
 * <p>Guarda o nome e o preço unitário <em>no momento da venda</em>: editar ou
 * excluir o produto depois não reescreve o histórico nem o faturamento.
 *
 * @param produtoId produto de origem, ou {@code null} se ele já foi excluído
 *                  do cadastro (a venda continua valendo).
 */
public record Venda(long id, Long produtoId, String nomeProduto, int quantidade,
                    long precoUnitarioCentavos, LocalDateTime data) {

    /** Id de uma venda ainda não gravada no banco. */
    public static final long SEM_ID = 0L;

    public Venda {
        if (nomeProduto == null || nomeProduto.isBlank())
            throw new RegraDeNegocioException("A venda precisa do nome do produto.");
        if (quantidade <= 0)
            throw new RegraDeNegocioException("A quantidade vendida deve ser maior que zero.");
        if (precoUnitarioCentavos < 0)
            throw new RegraDeNegocioException("O preço não pode ser negativo.");
        if (data == null)
            throw new RegraDeNegocioException("A venda precisa de uma data.");
        nomeProduto = nomeProduto.trim();
    }

    /** Venda de um produto, congelando nome e preço atuais. */
    public static Venda de(Produto produto, int quantidade, LocalDateTime data) {
        return new Venda(SEM_ID, produto.id(), produto.nome(), quantidade,
                produto.precoCentavos(), data);
    }

    public Venda comId(long novoId) {
        return new Venda(novoId, produtoId, nomeProduto, quantidade,
                precoUnitarioCentavos, data);
    }

    /** Vazio quando o produto de origem já não existe no cadastro. */
    public Optional<Long> produtoDeOrigem() {
        return Optional.ofNullable(produtoId);
    }

    public long totalCentavos() {
        return precoUnitarioCentavos * quantidade;
    }
}

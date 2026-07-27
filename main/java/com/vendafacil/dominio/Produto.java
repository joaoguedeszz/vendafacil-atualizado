package com.vendafacil.dominio;

import java.util.Locale;

/**
 * Produto do estoque — valor imutável.
 *
 * <p>Toda alteração devolve uma nova instância ({@link #baixarEstoque(int)},
 * {@link #reporEstoque(int)}…), de modo que um objeto em mãos nunca muda por
 * baixo de quem o segura. A validação mora no construtor: não existe
 * {@code Produto} inválido em memória.
 *
 * <p>O preço é guardado em centavos. Veja {@link Moeda}.
 */
public record Produto(long id, String nome, long precoCentavos, int quantidade) {

    /** Id de um produto ainda não gravado no banco. */
    public static final long SEM_ID = 0L;

    public static final int NOME_MAX = 120;
    public static final int QUANTIDADE_MAX = 1_000_000;

    public Produto {
        nome = exigirNomeValido(nome);
        if (precoCentavos < 0)
            throw new RegraDeNegocioException("O preço não pode ser negativo.");
        if (precoCentavos > Moeda.MAX_CENTAVOS)
            throw new RegraDeNegocioException("Valor muito alto.");
        if (quantidade < 0)
            throw new RegraDeNegocioException("A quantidade não pode ser negativa.");
        if (quantidade > QUANTIDADE_MAX)
            throw new RegraDeNegocioException(
                    "A quantidade máxima é " + QUANTIDADE_MAX + " unidades.");
    }

    /** Produto novo, ainda sem id — o repositório atribui um ao inserir. */
    public static Produto novo(String nome, long precoCentavos, int quantidade) {
        return new Produto(SEM_ID, nome, precoCentavos, quantidade);
    }

    public boolean persistido() {
        return id != SEM_ID;
    }

    /**
     * Nome em minúsculas, usado como chave de unicidade e de busca.
     *
     * <p>Feito em Java (e não com {@code COLLATE NOCASE}) porque o SQLite só
     * ignora maiúsculas em ASCII: "Café" e "CAFÉ" passariam como nomes
     * diferentes.
     */
    public String nomeNormalizado() {
        return normalizar(nome);
    }

    public static String normalizar(String nome) {
        return nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
    }

    public SituacaoEstoque situacao() {
        return SituacaoEstoque.de(quantidade);
    }

    public long valorEmEstoqueCentavos() {
        return precoCentavos * quantidade;
    }

    public Produto comId(long novoId) {
        return new Produto(novoId, nome, precoCentavos, quantidade);
    }

    public Produto comQuantidade(int novaQuantidade) {
        return new Produto(id, nome, precoCentavos, novaQuantidade);
    }

    public Produto comDados(String novoNome, long novoPreco, int novaQuantidade) {
        return new Produto(id, novoNome, novoPreco, novaQuantidade);
    }

    /** Baixa por venda. Nunca deixa o estoque negativo. */
    public Produto baixarEstoque(int qtd) {
        exigirPositivo(qtd);
        if (qtd > quantidade)
            throw new RegraDeNegocioException("Estoque insuficiente para \"" + nome
                    + "\": disponível " + quantidade + ", pedido " + qtd + ".");
        return comQuantidade(quantidade - qtd);
    }

    /** Devolução ao estoque (cancelamento de venda ou reposição). */
    public Produto reporEstoque(int qtd) {
        exigirPositivo(qtd);
        return comQuantidade(quantidade + qtd);
    }

    private static void exigirPositivo(int qtd) {
        if (qtd <= 0)
            throw new RegraDeNegocioException("A quantidade deve ser maior que zero.");
    }

    private static String exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank())
            throw new RegraDeNegocioException("O nome do produto é obrigatório.");
        String limpo = nome.trim();
        if (limpo.length() > NOME_MAX)
            throw new RegraDeNegocioException(
                    "O nome do produto deve ter no máximo " + NOME_MAX + " caracteres.");
        return limpo;
    }
}

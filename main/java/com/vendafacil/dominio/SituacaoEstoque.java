package com.vendafacil.dominio;

/** Faixa de estoque de um produto, usada para alertas e sinalização visual. */
public enum SituacaoEstoque {

    ESGOTADO("Sem estoque"),
    BAIXO("Estoque baixo"),
    NORMAL("Em estoque");

    /** Quantidade a partir da qual o produto ainda é considerado saudável. */
    public static final int LIMIAR_BAIXO = 5;

    private final String rotulo;

    SituacaoEstoque(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    public static SituacaoEstoque de(int quantidade) {
        if (quantidade <= 0) return ESGOTADO;
        if (quantidade <= LIMIAR_BAIXO) return BAIXO;
        return NORMAL;
    }
}

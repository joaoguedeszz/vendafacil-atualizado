package com.vendafacil.repositorio.jdbc;

import com.vendafacil.persistencia.BancoDados;
import com.vendafacil.repositorio.Transacoes;

import java.util.function.Supplier;

/** Liga a fronteira de transação dos serviços ao {@link BancoDados}. */
public final class TransacoesJdbc implements Transacoes {

    private final BancoDados banco;

    public TransacoesJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public <T> T executar(Supplier<T> acao) {
        return banco.emTransacao(acao::get);
    }

    @Override
    public void executar(Runnable acao) {
        banco.emTransacao(acao);
    }
}

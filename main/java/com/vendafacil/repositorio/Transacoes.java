package com.vendafacil.repositorio;

import java.util.function.Supplier;

/**
 * Fronteira de transação, vista pela camada de serviço.
 *
 * <p>Existe para que os serviços possam dizer "isto é uma operação só" sem
 * conhecer JDBC. Quem implementa é a camada de persistência.
 */
public interface Transacoes {

    /** Executa o bloco; confirma no fim ou desfaz tudo se algo for lançado. */
    <T> T executar(Supplier<T> acao);

    void executar(Runnable acao);
}

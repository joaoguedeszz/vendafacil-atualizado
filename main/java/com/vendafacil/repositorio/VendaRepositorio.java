package com.vendafacil.repositorio;

import com.vendafacil.dominio.Venda;

import java.util.List;
import java.util.Optional;

/** Acesso ao histórico de vendas. */
public interface VendaRepositorio {

    /** Grava uma venda nova e devolve a cópia com o id atribuído. */
    Venda inserir(Venda nova);

    /** @return false se não havia nada para excluir. */
    boolean excluir(long id);

    Optional<Venda> porId(long id);

    /** Todas as vendas, da mais recente para a mais antiga. */
    List<Venda> todas();

    /** As {@code limite} vendas mais recentes. */
    List<Venda> ultimas(int limite);

    int contar();

    long receitaTotalCentavos();
}

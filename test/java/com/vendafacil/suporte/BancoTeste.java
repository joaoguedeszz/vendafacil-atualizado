package com.vendafacil.suporte;

import com.vendafacil.persistencia.BancoDados;
import com.vendafacil.persistencia.Migrador;
import com.vendafacil.repositorio.ProdutoRepositorio;
import com.vendafacil.repositorio.Transacoes;
import com.vendafacil.repositorio.UsuarioRepositorio;
import com.vendafacil.repositorio.VendaRepositorio;
import com.vendafacil.repositorio.jdbc.ProdutoRepositorioJdbc;
import com.vendafacil.repositorio.jdbc.TransacoesJdbc;
import com.vendafacil.repositorio.jdbc.UsuarioRepositorioJdbc;
import com.vendafacil.repositorio.jdbc.VendaRepositorioJdbc;

/**
 * Banco SQLite em memória, migrado e com os repositórios prontos.
 *
 * <p>Os testes de serviço rodam contra o banco de verdade, não contra dublês:
 * é o que garante que constraints, transações e mapeamento estejam corretos.
 * Como é em memória, continua rápido.
 */
public final class BancoTeste implements AutoCloseable {

    private final BancoDados banco;
    private final Transacoes transacoes;
    private final ProdutoRepositorio produtos;
    private final VendaRepositorio vendas;
    private final UsuarioRepositorio usuarios;

    public BancoTeste() {
        this.banco = BancoDados.emMemoria();
        Migrador.aplicar(banco);
        this.transacoes = new TransacoesJdbc(banco);
        this.produtos = new ProdutoRepositorioJdbc(banco);
        this.vendas = new VendaRepositorioJdbc(banco);
        this.usuarios = new UsuarioRepositorioJdbc(banco);
    }

    public BancoDados banco() {
        return banco;
    }

    public Transacoes transacoes() {
        return transacoes;
    }

    public ProdutoRepositorio produtos() {
        return produtos;
    }

    public VendaRepositorio vendas() {
        return vendas;
    }

    public UsuarioRepositorio usuarios() {
        return usuarios;
    }

    @Override
    public void close() {
        banco.close();
    }
}

package com.vendafacil.repositorio.jdbc;

import com.vendafacil.dominio.Credencial;
import com.vendafacil.persistencia.BancoDados;
import com.vendafacil.repositorio.UsuarioRepositorio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** Implementação de {@link UsuarioRepositorio} sobre SQLite. */
public final class UsuarioRepositorioJdbc implements UsuarioRepositorio {

    private static final String COLUNAS =
            "id, login, hash_senha, salt, iteracoes, criado_em";

    private final BancoDados banco;

    public UsuarioRepositorioJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Credencial inserir(Credencial nova) {
        long id = banco.inserir("""
                INSERT INTO usuario (login, hash_senha, salt, iteracoes, criado_em)
                VALUES (?, ?, ?, ?, ?)
                """, ps -> {
            ps.setString(1, nova.login());
            ps.setString(2, nova.hashSenha());
            ps.setString(3, nova.salt());
            ps.setInt(4, nova.iteracoes());
            ps.setString(5, Datas.paraTexto(nova.criadoEm()));
        });
        return new Credencial(id, nova.login(), nova.hashSenha(), nova.salt(),
                nova.iteracoes(), nova.criadoEm());
    }

    @Override
    public Optional<Credencial> porLogin(String login) {
        return banco.buscarUm("SELECT " + COLUNAS + " FROM usuario WHERE login = ?",
                ps -> ps.setString(1, Credencial.normalizarLogin(login)),
                UsuarioRepositorioJdbc::mapear);
    }

    @Override
    public int contar() {
        return (int) banco.escalar("SELECT COUNT(*) FROM usuario");
    }

    private static Credencial mapear(ResultSet rs) throws SQLException {
        return new Credencial(rs.getLong("id"), rs.getString("login"),
                rs.getString("hash_senha"), rs.getString("salt"),
                rs.getInt("iteracoes"), Datas.deTexto(rs.getString("criado_em")));
    }
}

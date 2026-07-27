package com.vendafacil.repositorio.jdbc;

import com.vendafacil.dominio.Venda;
import com.vendafacil.persistencia.BancoDados;
import com.vendafacil.repositorio.VendaRepositorio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** Implementação de {@link VendaRepositorio} sobre SQLite. */
public final class VendaRepositorioJdbc implements VendaRepositorio {

    private static final String COLUNAS =
            "id, produto_id, nome_produto, quantidade, preco_unitario_centavos, data";

    /** Desempate por id: duas vendas podem cair no mesmo segundo. */
    private static final String ORDEM_RECENTES = " ORDER BY data DESC, id DESC";

    private final BancoDados banco;

    public VendaRepositorioJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Venda inserir(Venda nova) {
        long id = banco.inserir("""
                INSERT INTO venda
                    (produto_id, nome_produto, quantidade,
                     preco_unitario_centavos, data)
                VALUES (?, ?, ?, ?, ?)
                """, ps -> {
            if (nova.produtoId() == null) ps.setNull(1, Types.INTEGER);
            else ps.setLong(1, nova.produtoId());
            ps.setString(2, nova.nomeProduto());
            ps.setInt(3, nova.quantidade());
            ps.setLong(4, nova.precoUnitarioCentavos());
            ps.setString(5, Datas.paraTexto(nova.data()));
        });
        return nova.comId(id);
    }

    @Override
    public boolean excluir(long id) {
        return banco.executar("DELETE FROM venda WHERE id = ?",
                ps -> ps.setLong(1, id)) > 0;
    }

    @Override
    public Optional<Venda> porId(long id) {
        return banco.buscarUm("SELECT " + COLUNAS + " FROM venda WHERE id = ?",
                ps -> ps.setLong(1, id), VendaRepositorioJdbc::mapear);
    }

    @Override
    public List<Venda> todas() {
        return banco.listar("SELECT " + COLUNAS + " FROM venda" + ORDEM_RECENTES,
                BancoDados.Parametros.NENHUM, VendaRepositorioJdbc::mapear);
    }

    @Override
    public List<Venda> ultimas(int limite) {
        if (limite <= 0) return List.of();
        return banco.listar(
                "SELECT " + COLUNAS + " FROM venda" + ORDEM_RECENTES + " LIMIT ?",
                ps -> ps.setInt(1, limite), VendaRepositorioJdbc::mapear);
    }

    @Override
    public int contar() {
        return (int) banco.escalar("SELECT COUNT(*) FROM venda");
    }

    @Override
    public long receitaTotalCentavos() {
        return banco.escalar("""
                SELECT COALESCE(SUM(preco_unitario_centavos * quantidade), 0) FROM venda
                """);
    }

    private static Venda mapear(ResultSet rs) throws SQLException {
        long produtoId = rs.getLong("produto_id");
        // produto_id vira NULL quando o produto de origem é excluído.
        Long origem = rs.wasNull() ? null : produtoId;
        return new Venda(rs.getLong("id"), origem, rs.getString("nome_produto"),
                rs.getInt("quantidade"), rs.getLong("preco_unitario_centavos"),
                Datas.deTexto(rs.getString("data")));
    }
}

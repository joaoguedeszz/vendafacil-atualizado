package com.vendafacil.repositorio.jdbc;

import com.vendafacil.dominio.Produto;
import com.vendafacil.persistencia.BancoDados;
import com.vendafacil.repositorio.ProdutoRepositorio;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Implementação de {@link ProdutoRepositorio} sobre SQLite. */
public final class ProdutoRepositorioJdbc implements ProdutoRepositorio {

    private static final String COLUNAS = "id, nome, preco_centavos, quantidade";

    private final BancoDados banco;

    public ProdutoRepositorioJdbc(BancoDados banco) {
        this.banco = banco;
    }

    @Override
    public Produto inserir(Produto novo) {
        String agora = Datas.paraTexto(LocalDateTime.now());
        long id = banco.inserir("""
                INSERT INTO produto
                    (nome, nome_busca, preco_centavos, quantidade, criado_em, atualizado_em)
                VALUES (?, ?, ?, ?, ?, ?)
                """, ps -> {
            ps.setString(1, novo.nome());
            ps.setString(2, novo.nomeNormalizado());
            ps.setLong(3, novo.precoCentavos());
            ps.setInt(4, novo.quantidade());
            ps.setString(5, agora);
            ps.setString(6, agora);
        });
        return novo.comId(id);
    }

    @Override
    public boolean atualizar(Produto produto) {
        int linhas = banco.executar("""
                UPDATE produto
                   SET nome = ?, nome_busca = ?, preco_centavos = ?,
                       quantidade = ?, atualizado_em = ?
                 WHERE id = ?
                """, ps -> {
            ps.setString(1, produto.nome());
            ps.setString(2, produto.nomeNormalizado());
            ps.setLong(3, produto.precoCentavos());
            ps.setInt(4, produto.quantidade());
            ps.setString(5, Datas.paraTexto(LocalDateTime.now()));
            ps.setLong(6, produto.id());
        });
        return linhas > 0;
    }

    @Override
    public boolean excluir(long id) {
        return banco.executar("DELETE FROM produto WHERE id = ?",
                ps -> ps.setLong(1, id)) > 0;
    }

    @Override
    public Optional<Produto> porId(long id) {
        return banco.buscarUm("SELECT " + COLUNAS + " FROM produto WHERE id = ?",
                ps -> ps.setLong(1, id), ProdutoRepositorioJdbc::mapear);
    }

    @Override
    public List<Produto> todos() {
        return banco.listar("SELECT " + COLUNAS + " FROM produto ORDER BY nome_busca",
                BancoDados.Parametros.NENHUM, ProdutoRepositorioJdbc::mapear);
    }

    @Override
    public List<Produto> buscarPorNome(String termo) {
        String alvo = Produto.normalizar(termo);
        if (alvo.isEmpty()) return todos();
        return banco.listar("""
                SELECT %s FROM produto
                 WHERE nome_busca LIKE ? ESCAPE '\\'
                 ORDER BY nome_busca
                """.formatted(COLUNAS),
                ps -> ps.setString(1, "%" + escaparLike(alvo) + "%"),
                ProdutoRepositorioJdbc::mapear);
    }

    @Override
    public List<Produto> comEstoqueAte(int limiar) {
        return banco.listar("""
                SELECT %s FROM produto
                 WHERE quantidade <= ?
                 ORDER BY quantidade, nome_busca
                """.formatted(COLUNAS),
                ps -> ps.setInt(1, limiar), ProdutoRepositorioJdbc::mapear);
    }

    @Override
    public List<Produto> disponiveis() {
        return banco.listar("""
                SELECT %s FROM produto WHERE quantidade > 0 ORDER BY nome_busca
                """.formatted(COLUNAS),
                BancoDados.Parametros.NENHUM, ProdutoRepositorioJdbc::mapear);
    }

    @Override
    public Optional<Produto> porNome(String nome, long idIgnorado) {
        return banco.buscarUm("""
                SELECT %s FROM produto WHERE nome_busca = ? AND id <> ?
                """.formatted(COLUNAS), ps -> {
            ps.setString(1, Produto.normalizar(nome));
            ps.setLong(2, idIgnorado);
        }, ProdutoRepositorioJdbc::mapear);
    }

    @Override
    public int contar() {
        return (int) banco.escalar("SELECT COUNT(*) FROM produto");
    }

    @Override
    public long somaUnidades() {
        return banco.escalar("SELECT COALESCE(SUM(quantidade), 0) FROM produto");
    }

    @Override
    public long somaValorEstoqueCentavos() {
        return banco.escalar(
                "SELECT COALESCE(SUM(preco_centavos * quantidade), 0) FROM produto");
    }

    private static Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(rs.getLong("id"), rs.getString("nome"),
                rs.getLong("preco_centavos"), rs.getInt("quantidade"));
    }

    /** Neutraliza os curingas do LIKE para que "50%" seja buscado literalmente. */
    private static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

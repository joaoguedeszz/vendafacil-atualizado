package com.vendafacil.persistencia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Conexão com o banco SQLite e utilitários de acesso.
 *
 * <p>O VendaFácil é um aplicativo local de usuário único, então mantém uma
 * única conexão aberta durante toda a sessão — abrir e fechar a cada consulta
 * custaria mais do que rende. Todos os métodos são {@code synchronized}: o
 * Swing chama a partir da EDT, mas o importador de dados legados e os testes
 * podem chamar de outra thread, e uma conexão JDBC não é thread-safe.
 *
 * <p>As chamadas SQL ficam nos repositórios; aqui só existe o encanamento
 * (transações, mapeamento de {@link ResultSet}, tradução de {@link
 * SQLException} para {@link PersistenciaException}).
 */
public final class BancoDados implements AutoCloseable {

    /** Aplica os parâmetros de um {@code ?}-statement. */
    @FunctionalInterface
    public interface Parametros {
        void aplicar(PreparedStatement ps) throws SQLException;

        Parametros NENHUM = ps -> {};
    }

    /** Converte a linha corrente do {@link ResultSet} em um objeto. */
    @FunctionalInterface
    public interface Mapeador<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    /** Bloco a executar dentro de uma transação. */
    @FunctionalInterface
    public interface Transacao<T> {
        T executar() throws SQLException;
    }

    private final Connection conexao;
    private int profundidadeTransacao;
    private boolean transacaoAbortada;

    private BancoDados(Connection conexao) {
        this.conexao = conexao;
    }

    /**
     * Abre (criando se necessário) o banco no arquivo indicado.
     * O diretório é criado junto.
     */
    public static BancoDados abrir(Path arquivo) {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new PersistenciaException(
                    "Não foi possível criar a pasta de dados: " + arquivo.getParent(), e);
        }
        return conectar("jdbc:sqlite:" + arquivo.toAbsolutePath());
    }

    /** Banco temporário em memória — vive enquanto a conexão estiver aberta. */
    public static BancoDados emMemoria() {
        return conectar("jdbc:sqlite::memory:");
    }

    private static BancoDados conectar(String url) {
        try {
            Connection c = DriverManager.getConnection(url);
            try (Statement st = c.createStatement()) {
                // Sem isto o SQLite aceita FK penduradas silenciosamente.
                st.execute("PRAGMA foreign_keys = ON");
                // WAL: leitura não bloqueia escrita e o arquivo aguenta queda de energia.
                st.execute("PRAGMA journal_mode = WAL");
                st.execute("PRAGMA synchronous = NORMAL");
                st.execute("PRAGMA busy_timeout = 5000");
            }
            return new BancoDados(c);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível abrir o banco de dados.", e);
        }
    }

    // ------------------------------------------------------------------
    // Transações
    // ------------------------------------------------------------------

    /**
     * Executa o bloco em uma transação, confirmando no fim ou desfazendo tudo
     * se algo for lançado.
     *
     * <p>Chamadas aninhadas participam da transação externa — só a mais
     * externa confirma. É o que permite {@code VendaServico.registrar} juntar
     * "baixar estoque" e "gravar venda" em uma operação só.
     */
    public synchronized <T> T emTransacao(Transacao<T> acao) {
        boolean raiz = profundidadeTransacao == 0;
        try {
            if (raiz) {
                conexao.setAutoCommit(false);
                transacaoAbortada = false;
            }
            profundidadeTransacao++;
            T resultado = acao.executar();
            profundidadeTransacao--;
            if (raiz && !transacaoAbortada) {
                conexao.commit();
                conexao.setAutoCommit(true);
            }
            return resultado;
        } catch (SQLException e) {
            profundidadeTransacao--;
            desfazer(raiz);
            throw new PersistenciaException("Falha ao gravar no banco de dados.", e);
        } catch (RuntimeException e) {
            profundidadeTransacao--;
            desfazer(raiz);
            throw e;
        }
    }

    /** Variante sem valor de retorno. */
    public synchronized void emTransacao(Runnable acao) {
        emTransacao(() -> {
            acao.run();
            return null;
        });
    }

    private void desfazer(boolean raiz) {
        transacaoAbortada = true;
        if (!raiz) return;
        try {
            conexao.rollback();
        } catch (SQLException ignorada) {
            // Nada a fazer: a falha original é a que interessa.
        } finally {
            try {
                conexao.setAutoCommit(true);
            } catch (SQLException ignorada) {
                // idem
            }
            transacaoAbortada = false;
        }
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    public synchronized <T> List<T> listar(String sql, Parametros parametros,
                                           Mapeador<T> mapeador) {
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            parametros.aplicar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (rs.next()) resultado.add(mapeador.mapear(rs));
                return resultado;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar o banco de dados.", e);
        }
    }

    public synchronized <T> Optional<T> buscarUm(String sql, Parametros parametros,
                                                 Mapeador<T> mapeador) {
        List<T> encontrados = listar(sql, parametros, mapeador);
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.get(0));
    }

    /** Consulta que devolve um único número (COUNT, SUM, PRAGMA…). */
    public synchronized long escalar(String sql, Parametros parametros) {
        return buscarUm(sql, parametros, rs -> rs.getLong(1)).orElse(0L);
    }

    public synchronized long escalar(String sql) {
        return escalar(sql, Parametros.NENHUM);
    }

    // ------------------------------------------------------------------
    // Escrita
    // ------------------------------------------------------------------

    /** @return número de linhas afetadas. */
    public synchronized int executar(String sql, Parametros parametros) {
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            parametros.aplicar(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao gravar no banco de dados.", e);
        }
    }

    /** INSERT que devolve a chave gerada. */
    public synchronized long inserir(String sql, Parametros parametros) {
        try (PreparedStatement ps =
                     conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametros.aplicar(ps);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) return chaves.getLong(1);
            }
            throw new PersistenciaException("O banco não devolveu o id do registro criado.");
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao gravar no banco de dados.", e);
        }
    }

    /** Comando avulso, sem parâmetros (DDL, PRAGMA de escrita). */
    public synchronized void comando(String sql) {
        try (Statement st = conexao.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao executar: " + sql, e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (!conexao.isClosed()) conexao.close();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao fechar o banco de dados.", e);
        }
    }
}

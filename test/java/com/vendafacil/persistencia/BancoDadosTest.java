package com.vendafacil.persistencia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BancoDadosTest {

    private BancoDados banco;

    @BeforeEach
    void abrir() {
        banco = BancoDados.emMemoria();
        banco.comando("CREATE TABLE item (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT)");
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    private long contarItens() {
        return banco.escalar("SELECT COUNT(*) FROM item");
    }

    private void inserirItem(String nome) {
        banco.inserir("INSERT INTO item (nome) VALUES (?)", ps -> ps.setString(1, nome));
    }

    @Test
    @DisplayName("chaves estrangeiras estão ligadas na conexão")
    void ativaChavesEstrangeiras() {
        assertEquals(1, banco.escalar("PRAGMA foreign_keys"));
    }

    @Test
    @DisplayName("inserir devolve a chave gerada")
    void devolveChaveGerada() {
        long primeiro = banco.inserir("INSERT INTO item (nome) VALUES (?)",
                ps -> ps.setString(1, "a"));
        long segundo = banco.inserir("INSERT INTO item (nome) VALUES (?)",
                ps -> ps.setString(1, "b"));

        assertTrue(primeiro > 0);
        assertEquals(primeiro + 1, segundo);
    }

    @Test
    @DisplayName("transação confirmada mantém tudo")
    void confirmaTransacao() {
        banco.emTransacao(() -> {
            inserirItem("a");
            inserirItem("b");
        });

        assertEquals(2, contarItens());
    }

    @Test
    @DisplayName("exceção no meio desfaz a transação inteira")
    void desfazTransacaoComErro() {
        assertThrows(IllegalStateException.class, () -> banco.emTransacao(() -> {
            inserirItem("a");
            throw new IllegalStateException("falha simulada");
        }));

        assertEquals(0, contarItens(), "a primeira inserção também deve sumir");
    }

    @Test
    @DisplayName("o banco volta a funcionar depois de um rollback")
    void continuaUsavelAposRollback() {
        assertThrows(IllegalStateException.class, () -> banco.emTransacao(() -> {
            inserirItem("a");
            throw new IllegalStateException("falha simulada");
        }));

        inserirItem("depois");

        assertEquals(1, contarItens());
    }

    @Test
    @DisplayName("transações aninhadas confirmam só na mais externa")
    void aninhaTransacoes() {
        banco.emTransacao(() -> {
            inserirItem("externa");
            banco.emTransacao(() -> inserirItem("interna"));
        });

        assertEquals(2, contarItens());
    }

    @Test
    @DisplayName("falha na transação interna desfaz também a externa")
    void desfazAninhadaInteira() {
        assertThrows(IllegalStateException.class, () -> banco.emTransacao(() -> {
            inserirItem("externa");
            banco.emTransacao(() -> {
                inserirItem("interna");
                throw new IllegalStateException("falha simulada");
            });
        }));

        assertEquals(0, contarItens());
    }

    @Test
    @DisplayName("transação com retorno entrega o valor")
    void transacaoComRetorno() {
        long id = banco.emTransacao(() -> banco.inserir(
                "INSERT INTO item (nome) VALUES (?)", ps -> ps.setString(1, "x")));

        assertTrue(id > 0);
        assertEquals(1, contarItens());
    }

    @Test
    @DisplayName("SQL inválido vira PersistenciaException, não SQLException crua")
    void traduzErroDeSql() {
        assertThrows(PersistenciaException.class,
                () -> banco.escalar("SELECT * FROM tabela_que_nao_existe"));
        assertThrows(PersistenciaException.class,
                () -> banco.executar("INSERT INTO item (coluna_errada) VALUES (1)",
                        BancoDados.Parametros.NENHUM));
    }

    @Test
    @DisplayName("buscarUm devolve vazio quando não há linha")
    void buscarUmSemResultado() {
        assertTrue(banco.buscarUm("SELECT nome FROM item WHERE id = ?",
                ps -> ps.setLong(1, 999), rs -> rs.getString(1)).isEmpty());
    }
}

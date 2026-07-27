package com.vendafacil.persistencia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigradorTest {

    @Test
    @DisplayName("cria o esquema e registra a versão no arquivo")
    void aplicaMigracoes() {
        try (BancoDados banco = BancoDados.emMemoria()) {
            assertEquals(0, banco.escalar("PRAGMA user_version"));

            int versao = Migrador.aplicar(banco);

            assertEquals(Migrador.versaoAlvo(), versao);
            assertEquals(Migrador.versaoAlvo(), banco.escalar("PRAGMA user_version"));
            assertEquals(3, banco.escalar("""
                    SELECT COUNT(*) FROM sqlite_master
                     WHERE type = 'table' AND name IN ('produto', 'venda', 'usuario')
                    """));
        }
    }

    @Test
    @DisplayName("rodar de novo não repete nada")
    void ehIdempotente() {
        try (BancoDados banco = BancoDados.emMemoria()) {
            Migrador.aplicar(banco);
            banco.executar("""
                    INSERT INTO produto
                        (nome, nome_busca, preco_centavos, quantidade,
                         criado_em, atualizado_em)
                    VALUES ('Café', 'café', 100, 1, '2026-01-01T00:00:00',
                            '2026-01-01T00:00:00')
                    """, BancoDados.Parametros.NENHUM);

            Migrador.aplicar(banco);

            assertEquals(1, banco.escalar("SELECT COUNT(*) FROM produto"),
                    "o dado existente não pode ser perdido");
        }
    }

    @Test
    @DisplayName("recusa arquivo de uma versão futura em vez de corromper")
    void recusaEsquemaMaisNovo() {
        try (BancoDados banco = BancoDados.emMemoria()) {
            banco.comando("PRAGMA user_version = " + (Migrador.versaoAlvo() + 5));

            PersistenciaException erro =
                    assertThrows(PersistenciaException.class, () -> Migrador.aplicar(banco));

            assertTrue(erro.getMessage().contains("versão mais nova"));
        }
    }

    @Test
    @DisplayName("o banco em arquivo persiste entre aberturas")
    void persisteEmArquivo(@TempDir Path pasta) {
        Path arquivo = pasta.resolve("sub").resolve("vendafacil.db");

        try (BancoDados banco = BancoDados.abrir(arquivo)) {
            Migrador.aplicar(banco);
            banco.executar("""
                    INSERT INTO produto
                        (nome, nome_busca, preco_centavos, quantidade,
                         criado_em, atualizado_em)
                    VALUES ('Café', 'café', 100, 1, '2026-01-01T00:00:00',
                            '2026-01-01T00:00:00')
                    """, BancoDados.Parametros.NENHUM);
        }

        assertTrue(Files.exists(arquivo), "a pasta deve ser criada junto com o arquivo");

        try (BancoDados reaberto = BancoDados.abrir(arquivo)) {
            assertEquals(Migrador.versaoAlvo(), Migrador.aplicar(reaberto));
            assertEquals(1, reaberto.escalar("SELECT COUNT(*) FROM produto"));
        }
    }

    @Test
    @DisplayName("as restrições do esquema barram dados inválidos")
    void aplicaRestricoes() {
        try (BancoDados banco = BancoDados.emMemoria()) {
            Migrador.aplicar(banco);

            assertThrows(PersistenciaException.class, () -> banco.executar("""
                    INSERT INTO produto
                        (nome, nome_busca, preco_centavos, quantidade,
                         criado_em, atualizado_em)
                    VALUES ('Café', 'café', -1, 1, '2026-01-01T00:00:00',
                            '2026-01-01T00:00:00')
                    """, BancoDados.Parametros.NENHUM), "preço negativo");

            assertThrows(PersistenciaException.class, () -> banco.executar("""
                    INSERT INTO venda
                        (produto_id, nome_produto, quantidade,
                         preco_unitario_centavos, data)
                    VALUES (NULL, 'Café', 0, 100, '2026-01-01T00:00:00')
                    """, BancoDados.Parametros.NENHUM), "venda com quantidade zero");
        }
    }
}

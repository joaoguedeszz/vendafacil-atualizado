package com.vendafacil.persistencia;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Versionamento do esquema do banco.
 *
 * <p>A versão aplicada fica no {@code PRAGMA user_version} do próprio arquivo,
 * então o banco carrega consigo a informação de quão atualizado está. Ao subir,
 * o aplicativo roda apenas as migrações que faltam, na ordem.
 *
 * <p>Para adicionar uma migração: crie
 * {@code src/main/resources/db/migracao/V<n>__descricao.sql} e acrescente o
 * nome ao fim de {@link #MIGRACOES}. Migrações já publicadas nunca são
 * editadas — bancos existentes não as rodariam de novo.
 */
public final class Migrador {

    private static final String PASTA = "/db/migracao/";

    /** Ordem importa: o índice + 1 é o número da versão. */
    private static final String[] MIGRACOES = {
            "V1__esquema_inicial.sql",
    };

    private Migrador() {}

    /**
     * Aplica as migrações pendentes.
     *
     * @return a versão do esquema depois da execução.
     */
    public static int aplicar(BancoDados banco) {
        int versaoAtual = (int) banco.escalar("PRAGMA user_version");
        if (versaoAtual > MIGRACOES.length) {
            throw new PersistenciaException(
                    "Este arquivo de dados foi criado por uma versão mais nova do "
                            + "VendaFácil (esquema v" + versaoAtual + ", suportado até v"
                            + MIGRACOES.length + "). Atualize o programa.");
        }
        for (int versao = versaoAtual; versao < MIGRACOES.length; versao++) {
            String arquivo = MIGRACOES[versao];
            List<String> comandos = separarComandos(lerRecurso(PASTA + arquivo));
            banco.emTransacao(() -> {
                for (String comando : comandos) banco.comando(comando);
            });
            // Fora da transação: PRAGMA de escrita não participa de rollback.
            banco.comando("PRAGMA user_version = " + (versao + 1));
        }
        return MIGRACOES.length;
    }

    /** Versão de esquema que este código conhece. */
    public static int versaoAlvo() {
        return MIGRACOES.length;
    }

    private static String lerRecurso(String caminho) {
        try (InputStream entrada = Migrador.class.getResourceAsStream(caminho)) {
            if (entrada == null)
                throw new PersistenciaException("Migração não encontrada: " + caminho);
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao ler a migração " + caminho, e);
        }
    }

    /**
     * Quebra o script em comandos individuais — o driver do SQLite executa um
     * por chamada. Comentários de linha são removidos antes da divisão para
     * que um {@code ;} dentro deles não parta o comando ao meio.
     */
    private static List<String> separarComandos(String script) {
        StringBuilder limpo = new StringBuilder();
        for (String linha : script.split("\n")) {
            int comentario = linha.indexOf("--");
            limpo.append(comentario >= 0 ? linha.substring(0, comentario) : linha)
                 .append('\n');
        }
        List<String> comandos = new ArrayList<>();
        for (String comando : limpo.toString().split(";")) {
            if (!comando.isBlank()) comandos.add(comando.trim());
        }
        return comandos;
    }
}

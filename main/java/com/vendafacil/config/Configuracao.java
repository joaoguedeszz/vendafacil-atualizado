package com.vendafacil.config;

import java.nio.file.Path;

/**
 * Onde o aplicativo guarda os dados.
 *
 * <p>Por padrão {@code ~/.vendafacil}. Pode ser redirecionado com
 * {@code -Dvendafacil.dados.dir=<pasta>} — usado pelos testes e por quem
 * queira manter os dados em um pendrive ou pasta sincronizada.
 */
public record Configuracao(Path diretorioDados) {

    public static final String PROPRIEDADE_DIRETORIO = "vendafacil.dados.dir";

    private static final String PASTA_PADRAO = ".vendafacil";
    private static final String ARQUIVO_BANCO = "vendafacil.db";
    private static final String ARQUIVO_LEGADO = "dados.txt";

    public static Configuracao padrao() {
        String configurado = System.getProperty(PROPRIEDADE_DIRETORIO);
        Path pasta = configurado != null && !configurado.isBlank()
                ? Path.of(configurado)
                : Path.of(System.getProperty("user.home"), PASTA_PADRAO);
        return new Configuracao(pasta);
    }

    /** Banco SQLite com produtos, vendas e usuários. */
    public Path arquivoBanco() {
        return diretorioDados.resolve(ARQUIVO_BANCO);
    }

    /** Arquivo texto das versões 1.x, importado automaticamente se existir. */
    public Path arquivoLegado() {
        return diretorioDados.resolve(ARQUIVO_LEGADO);
    }
}

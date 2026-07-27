package com.vendafacil.app;

import com.vendafacil.config.Configuracao;
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
import com.vendafacil.servico.AutenticacaoServico;
import com.vendafacil.servico.ImportadorLegado;
import com.vendafacil.servico.ProdutoServico;
import com.vendafacil.servico.RelatorioServico;
import com.vendafacil.servico.VendaServico;

/**
 * Montagem da aplicação: abre o banco, aplica as migrações e liga
 * repositórios e serviços.
 *
 * <p>É o único ponto do sistema que conhece todas as camadas ao mesmo tempo.
 * A interface recebe serviços prontos e não sabe que existe SQLite; os
 * serviços recebem repositórios e não sabem quem os implementa.
 */
public final class Contexto implements AutoCloseable {

    private final BancoDados banco;
    private final ProdutoServico produtos;
    private final VendaServico vendas;
    private final RelatorioServico relatorios;
    private final AutenticacaoServico autenticacao;
    private final ImportadorLegado.Resultado importacaoLegado;

    private Contexto(BancoDados banco, ProdutoServico produtos, VendaServico vendas,
                     RelatorioServico relatorios, AutenticacaoServico autenticacao,
                     ImportadorLegado.Resultado importacaoLegado) {
        this.banco = banco;
        this.produtos = produtos;
        this.vendas = vendas;
        this.relatorios = relatorios;
        this.autenticacao = autenticacao;
        this.importacaoLegado = importacaoLegado;
    }

    /** Abre o banco no diretório configurado e deixa tudo pronto para uso. */
    public static Contexto iniciar(Configuracao config) {
        return montar(BancoDados.abrir(config.arquivoBanco()), config);
    }

    /** Contexto descartável em memória — para testes. */
    public static Contexto emMemoria() {
        return montar(BancoDados.emMemoria(), null);
    }

    private static Contexto montar(BancoDados banco, Configuracao config) {
        try {
            Migrador.aplicar(banco);

            Transacoes transacoes = new TransacoesJdbc(banco);
            ProdutoRepositorio produtoRepo = new ProdutoRepositorioJdbc(banco);
            VendaRepositorio vendaRepo = new VendaRepositorioJdbc(banco);
            UsuarioRepositorio usuarioRepo = new UsuarioRepositorioJdbc(banco);

            // Antes de qualquer coisa: recuperar os dados da versão anterior.
            ImportadorLegado.Resultado importacao = config == null
                    ? null
                    : new ImportadorLegado(transacoes, produtoRepo, vendaRepo)
                            .importarSeNecessario(config.arquivoLegado());

            AutenticacaoServico autenticacao = new AutenticacaoServico(usuarioRepo);
            autenticacao.garantirUsuarioPadrao();

            return new Contexto(banco,
                    new ProdutoServico(produtoRepo),
                    new VendaServico(transacoes, produtoRepo, vendaRepo),
                    new RelatorioServico(produtoRepo, vendaRepo),
                    autenticacao,
                    importacao);
        } catch (RuntimeException e) {
            banco.close();
            throw e;
        }
    }

    public ProdutoServico produtos() {
        return produtos;
    }

    public VendaServico vendas() {
        return vendas;
    }

    public RelatorioServico relatorios() {
        return relatorios;
    }

    public AutenticacaoServico autenticacao() {
        return autenticacao;
    }

    /** Resultado da importação do arquivo antigo, ou {@code null} se não houve. */
    public ImportadorLegado.Resultado importacaoLegado() {
        return importacaoLegado;
    }

    @Override
    public void close() {
        banco.close();
    }
}

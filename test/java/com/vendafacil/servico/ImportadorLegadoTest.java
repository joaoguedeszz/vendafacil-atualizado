package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.Venda;
import com.vendafacil.suporte.BancoTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Garante que ninguém perde dados ao atualizar da versão 1.x para a 2.0. */
class ImportadorLegadoTest {

    @TempDir
    Path pasta;

    private BancoTeste banco;
    private ImportadorLegado importador;

    @BeforeEach
    void abrir() {
        banco = new BancoTeste();
        importador = new ImportadorLegado(banco.transacoes(), banco.produtos(),
                banco.vendas());
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    private Path escreverArquivoLegado(String conteudo) throws IOException {
        Path arquivo = pasta.resolve("dados.txt");
        Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        return arquivo;
    }

    @Test
    @DisplayName("importa produtos e vendas do arquivo texto")
    void importaArquivoCompleto() throws IOException {
        Path arquivo = escreverArquivoLegado("""
                #VENDAFACIL v1
                P;1;Caf%C3%A9+500g;1250;10
                P;2;Ch%C3%A1+verde;800;3
                V;3;1;Caf%C3%A9+500g;2;1250;2026-01-15T10:30:00
                """);

        ImportadorLegado.Resultado resultado = importador.importarSeNecessario(arquivo);

        assertTrue(resultado.executada());
        assertEquals(2, resultado.produtos());
        assertEquals(1, resultado.vendas());
        assertEquals(0, resultado.linhasIgnoradas());

        // Nome e acentuação sobrevivem ao URL-encoding do formato antigo.
        Produto café = banco.produtos().buscarPorNome("café").get(0);
        assertEquals("Café 500g", café.nome());
        assertEquals(1250, café.precoCentavos());
        assertEquals(10, café.quantidade());

        Venda venda = banco.vendas().todas().get(0);
        assertEquals("Café 500g", venda.nomeProduto());
        assertEquals(2, venda.quantidade());
        assertEquals(LocalDateTime.of(2026, 1, 15, 10, 30), venda.data());
    }

    @Test
    @DisplayName("o vínculo da venda aponta para o produto recriado")
    void religaVendaAoProduto() throws IOException {
        Path arquivo = escreverArquivoLegado("""
                P;7;Caf%C3%A9;1000;5
                V;8;7;Caf%C3%A9;1;1000;2026-01-15T10:30:00
                """);

        importador.importarSeNecessario(arquivo);

        Produto café = banco.produtos().todos().get(0);
        Venda venda = banco.vendas().todas().get(0);
        assertEquals(café.id(), venda.produtoDeOrigem().orElseThrow(),
                "o id novo do banco, não o id 7 do arquivo antigo");
    }

    @Test
    @DisplayName("venda de produto ausente entra sem vínculo, não é descartada")
    void importaVendaOrfa() throws IOException {
        Path arquivo = escreverArquivoLegado(
                "V;8;99;Produto+sumido;1;1000;2026-01-15T10:30:00\n");

        ImportadorLegado.Resultado resultado = importador.importarSeNecessario(arquivo);

        assertEquals(1, resultado.vendas());
        assertTrue(banco.vendas().todas().get(0).produtoDeOrigem().isEmpty());
    }

    @Test
    @DisplayName("linhas corrompidas são puladas sem derrubar o resto")
    void ignoraLinhasCorrompidas() throws IOException {
        Path arquivo = escreverArquivoLegado("""
                #VENDAFACIL v1
                P;1;Caf%C3%A9;1250;10
                P;lixo;sem;numero
                P;2;Sem+preco;abc;5
                X;9;desconhecido
                P;3;Ch%C3%A1;800;2

                """);

        ImportadorLegado.Resultado resultado = importador.importarSeNecessario(arquivo);

        assertEquals(2, resultado.produtos(), "Café e Chá devem entrar");
        assertEquals(3, resultado.linhasIgnoradas());
    }

    @Test
    @DisplayName("o arquivo antigo é arquivado, nunca apagado")
    void arquivaOriginal() throws IOException {
        Path arquivo = escreverArquivoLegado("P;1;Caf%C3%A9;1250;10\n");

        ImportadorLegado.Resultado resultado = importador.importarSeNecessario(arquivo);

        assertFalse(Files.exists(arquivo), "o original sai do caminho");
        assertTrue(Files.exists(resultado.arquivoArquivado()));
        assertTrue(resultado.arquivoArquivado().getFileName().toString()
                .endsWith(".importado"));
    }

    @Test
    @DisplayName("não importa duas vezes: na segunda abertura o banco já tem dados")
    void naoImportaSobreBancoComDados() throws IOException {
        Path arquivo = escreverArquivoLegado("P;1;Caf%C3%A9;1250;10\n");
        importador.importarSeNecessario(arquivo);

        // Simula o arquivo reaparecendo com o banco já povoado.
        Path outraVez = escreverArquivoLegado("P;1;Outro;500;1\n");
        ImportadorLegado.Resultado segunda = importador.importarSeNecessario(outraVez);

        assertFalse(segunda.executada());
        assertEquals(1, banco.produtos().contar());
        assertTrue(Files.exists(outraVez), "sem importação, o arquivo fica onde está");
    }

    @Test
    @DisplayName("sem arquivo antigo, nada acontece")
    void semArquivoNaoFazNada() {
        ImportadorLegado.Resultado resultado =
                importador.importarSeNecessario(pasta.resolve("nao-existe.txt"));

        assertFalse(resultado.executada());
        assertEquals(0, banco.produtos().contar());
    }

    @Test
    @DisplayName("arquivo vazio é importado sem produzir nada")
    void arquivoVazio() throws IOException {
        Path arquivo = escreverArquivoLegado("#VENDAFACIL v1\n");

        ImportadorLegado.Resultado resultado = importador.importarSeNecessario(arquivo);

        assertTrue(resultado.executada());
        assertEquals(0, resultado.produtos());
        assertEquals(0, resultado.vendas());
    }
}

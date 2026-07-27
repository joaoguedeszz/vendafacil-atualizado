package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.Venda;
import com.vendafacil.repositorio.ProdutoRepositorio;
import com.vendafacil.repositorio.Transacoes;
import com.vendafacil.repositorio.VendaRepositorio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traz para o banco os dados do arquivo texto usado até a versão 1.x
 * ({@code ~/.vendafacil/dados.txt}).
 *
 * <p>Roda uma vez, na primeira abertura após a atualização, e só quando o
 * banco ainda está vazio — nunca mistura dados novos com os antigos. Ao
 * terminar, o arquivo original é renomeado (não apagado): se algo der errado,
 * ele continua lá.
 *
 * <p>Formato de origem, um registro por linha:
 * <pre>
 * P;id;nome;preco_centavos;quantidade
 * V;id;produto_id;nome;quantidade;preco_unitario_centavos;data_iso
 * </pre>
 * O nome vem codificado em URL-encoding porque {@code ;} era o separador.
 */
public final class ImportadorLegado {

    /** Resultado da importação, para relatar ao usuário. */
    public record Resultado(boolean executada, int produtos, int vendas,
                            int linhasIgnoradas, Path arquivoArquivado) {

        static Resultado naoExecutada() {
            return new Resultado(false, 0, 0, 0, null);
        }
    }

    private final Transacoes transacoes;
    private final ProdutoRepositorio produtos;
    private final VendaRepositorio vendas;

    public ImportadorLegado(Transacoes transacoes, ProdutoRepositorio produtos,
                            VendaRepositorio vendas) {
        this.transacoes = transacoes;
        this.produtos = produtos;
        this.vendas = vendas;
    }

    /** Importa se houver arquivo antigo e o banco estiver vazio. */
    public Resultado importarSeNecessario(Path arquivoLegado) {
        if (!Files.isRegularFile(arquivoLegado)) return Resultado.naoExecutada();
        if (produtos.contar() > 0 || vendas.contar() > 0) return Resultado.naoExecutada();

        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivoLegado, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + arquivoLegado, e);
        }

        Contador contador = transacoes.executar(() -> importarLinhas(linhas));
        Path arquivado = arquivar(arquivoLegado);
        return new Resultado(true, contador.produtos, contador.vendas,
                contador.ignoradas, arquivado);
    }

    private static final class Contador {
        int produtos;
        int vendas;
        int ignoradas;
    }

    private Contador importarLinhas(List<String> linhas) {
        Contador contador = new Contador();
        // O id antigo era uma sequência única para produtos e vendas; o banco
        // gera ids próprios, então as vendas precisam do de-para.
        Map<Long, Long> idsProdutos = new HashMap<>();

        for (String linha : linhas) {
            if (linha.isBlank() || linha.startsWith("#")) continue;
            String[] campos = linha.split(";", -1);
            try {
                if (campos[0].equals("P") && campos.length >= 5) {
                    long idAntigo = Long.parseLong(campos[1]);
                    Produto salvo = produtos.inserir(Produto.novo(decodificar(campos[2]),
                            Long.parseLong(campos[3]), Integer.parseInt(campos[4])));
                    idsProdutos.put(idAntigo, salvo.id());
                    contador.produtos++;
                } else if (campos[0].equals("V") && campos.length >= 7) {
                    // Se o produto de origem não veio junto, a venda entra
                    // sem vínculo — nome e preço dela já bastam.
                    Long produtoId = idsProdutos.get(Long.parseLong(campos[2]));
                    vendas.inserir(new Venda(Venda.SEM_ID, produtoId,
                            decodificar(campos[3]), Integer.parseInt(campos[4]),
                            Long.parseLong(campos[5]), lerData(campos[6])));
                    contador.vendas++;
                } else {
                    contador.ignoradas++;
                }
            } catch (RuntimeException linhaCorrompida) {
                // Uma linha estragada não pode impedir o resto de ser recuperado.
                contador.ignoradas++;
            }
        }
        return contador;
    }

    private static LocalDateTime lerData(String texto) {
        return LocalDateTime.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .truncatedTo(ChronoUnit.SECONDS);
    }

    private static String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }

    /** Guarda o arquivo original com sufixo, em vez de apagá-lo. */
    private static Path arquivar(Path arquivoLegado) {
        Path destino = arquivoLegado.resolveSibling(
                arquivoLegado.getFileName() + ".importado");
        try {
            return Files.move(arquivoLegado, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Os dados já estão no banco; não conseguir renomear não é fatal.
            // Na próxima abertura o banco não estará mais vazio e o arquivo
            // será ignorado de qualquer forma.
            return arquivoLegado;
        }
    }
}

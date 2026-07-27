package com.vendafacil.servico;

import com.vendafacil.dominio.Produto;
import com.vendafacil.suporte.BancoTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatorioServicoTest {

    private BancoTeste banco;
    private ProdutoServico produtos;
    private VendaServico vendas;
    private RelatorioServico relatorios;

    @BeforeEach
    void abrir() {
        banco = new BancoTeste();
        produtos = new ProdutoServico(banco.produtos());
        vendas = new VendaServico(banco.transacoes(), banco.produtos(), banco.vendas());
        relatorios = new RelatorioServico(banco.produtos(), banco.vendas());
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    @Test
    @DisplayName("banco vazio devolve tudo zerado, não nulo")
    void indicadoresVazios() {
        Indicadores numeros = relatorios.indicadores();

        assertEquals(0, numeros.receitaTotalCentavos());
        assertEquals(0, numeros.quantidadeVendas());
        assertEquals(0, numeros.unidadesEmEstoque());
        assertEquals(0, numeros.quantidadeProdutos());
        assertEquals(0, numeros.alertasEstoque());
        assertEquals(0, numeros.valorEstoqueCentavos());
    }

    @Test
    @DisplayName("consolida receita, estoque e alertas")
    void consolidaIndicadores() {
        Produto café = produtos.cadastrar("Café", 1000, 10);   // 10.000 em estoque
        produtos.cadastrar("Chá", 500, 2);                     //  1.000, alerta
        produtos.cadastrar("Bolacha", 300, 0);                 //      0, alerta
        vendas.registrar(café.id(), 3);                        // receita 3.000

        Indicadores numeros = relatorios.indicadores();

        assertEquals(3000, numeros.receitaTotalCentavos());
        assertEquals(1, numeros.quantidadeVendas());
        assertEquals(9, numeros.unidadesEmEstoque(), "7 café + 2 chá + 0 bolacha");
        assertEquals(3, numeros.quantidadeProdutos());
        assertEquals(2, numeros.alertasEstoque());
        assertEquals(8000, numeros.valorEstoqueCentavos(), "7×1000 + 2×500 + 0×300");
    }

    @Test
    @DisplayName("últimas vendas respeitam o limite pedido")
    void limitaUltimasVendas() {
        Produto café = produtos.cadastrar("Café", 100, 10);
        for (int i = 0; i < 5; i++) vendas.registrar(café.id(), 1);

        assertEquals(3, relatorios.ultimasVendas(3).size());
        assertEquals(5, relatorios.ultimasVendas(10).size());
        assertEquals(0, relatorios.ultimasVendas(0).size());
    }
}

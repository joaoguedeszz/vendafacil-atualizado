package com.vendafacil.ui;

import com.vendafacil.dominio.Moeda;
import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.SituacaoEstoque;
import com.vendafacil.dominio.Venda;
import com.vendafacil.servico.Indicadores;
import com.vendafacil.servico.RelatorioServico;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Visão geral do negócio: receita, vendas, estoque e alertas.
 */
public class PainelDashboard extends JPanel implements TelaPrincipal.PainelAtualizavel {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final RelatorioServico relatorios;
    private final JLabel valorReceita = valorGrande();
    private final JLabel valorVendas = valorGrande();
    private final JLabel valorItens = valorGrande();
    private final JLabel valorAlertas = valorGrande();
    private final JLabel legendaItens = legenda("—");
    private final JPanel listaEstoqueBaixo = new JPanel();
    private final JPanel listaUltimasVendas = new JPanel();

    public PainelDashboard(RelatorioServico relatorios) {
        this.relatorios = relatorios;
        setLayout(new BorderLayout(0, 20));
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Dashboard");
        titulo.setFont(Tema.fonte(Font.BOLD, 24));
        titulo.setForeground(Tema.TEXTO);
        JLabel subtitulo = new JLabel("Visão geral do seu negócio");
        subtitulo.setFont(Tema.fonte(Font.PLAIN, 14));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(subtitulo);
        add(titulos, BorderLayout.NORTH);

        JPanel corpo = new JPanel(new BorderLayout(0, 20));
        corpo.setOpaque(false);

        JPanel cartoesTopo = new JPanel(new GridLayout(1, 4, 16, 0));
        cartoesTopo.setOpaque(false);
        cartoesTopo.add(cartaoIndicador("Receita total", valorReceita,
                legenda("soma das vendas"), "dinheiro", Tema.VERDE, Tema.VERDE_CLARO));
        cartoesTopo.add(cartaoIndicador("Vendas", valorVendas,
                legenda("no histórico"), "carrinho", Tema.AZUL, Tema.AZUL_CLARO));
        cartoesTopo.add(cartaoIndicador("Itens em estoque", valorItens,
                legendaItens, "caixa", Tema.VERDE_HOVER, Tema.VERDE_CLARO));
        cartoesTopo.add(cartaoIndicador("Estoque baixo", valorAlertas,
                legenda(SituacaoEstoque.LIMIAR_BAIXO + " unid. ou menos"),
                "alerta", Tema.ALERTA, Tema.ALERTA_CLARO));
        corpo.add(cartoesTopo, BorderLayout.NORTH);

        JPanel cartoesBaixo = new JPanel(new GridLayout(1, 2, 16, 0));
        cartoesBaixo.setOpaque(false);
        cartoesBaixo.add(cartaoLista("Alerta de estoque",
                "Produtos que precisam de reposição", listaEstoqueBaixo));
        cartoesBaixo.add(cartaoLista("Últimas vendas",
                "Movimentações mais recentes", listaUltimasVendas));
        corpo.add(cartoesBaixo, BorderLayout.CENTER);

        add(corpo, BorderLayout.CENTER);
    }

    @Override public JComponent componente() { return this; }

    @Override
    public void atualizar() {
        Indicadores numeros = relatorios.indicadores();
        valorReceita.setText(Moeda.formatar(numeros.receitaTotalCentavos()));
        valorVendas.setText(String.valueOf(numeros.quantidadeVendas()));
        valorItens.setText(String.valueOf(numeros.unidadesEmEstoque()));
        legendaItens.setText(numeros.quantidadeProdutos() == 1 ? "1 produto"
                : numeros.quantidadeProdutos() + " produtos");
        valorAlertas.setText(String.valueOf(numeros.alertasEstoque()));

        listaEstoqueBaixo.removeAll();
        List<Produto> baixos = relatorios.alertasEstoque();
        if (baixos.isEmpty()) {
            listaEstoqueBaixo.add(estadoVazio("check", Tema.VERDE,
                    "Estoque saudável — nenhum alerta."));
        } else {
            for (Produto p : baixos) {
                Tema.EtiquetaPill pill = new Tema.EtiquetaPill();
                if (p.situacao() == SituacaoEstoque.ESGOTADO)
                    pill.configurar("esgotado", Tema.PERIGO_CLARO, Tema.PERIGO);
                else
                    pill.configurar(p.quantidade() + " unid.",
                            Tema.ALERTA_CLARO, Tema.ALERTA);
                listaEstoqueBaixo.add(linhaLista(p.nome(),
                        Moeda.formatar(p.precoCentavos()), pill));
            }
        }
        listaEstoqueBaixo.revalidate();
        listaEstoqueBaixo.repaint();

        listaUltimasVendas.removeAll();
        List<Venda> ultimas = relatorios.ultimasVendas(6);
        if (ultimas.isEmpty()) {
            listaUltimasVendas.add(estadoVazio("carrinho", Tema.TEXTO_SUAVE,
                    "Nenhuma venda registrada ainda."));
        } else {
            for (Venda v : ultimas) {
                JLabel valor = new JLabel(Moeda.formatar(v.totalCentavos()));
                valor.setFont(Tema.fonte(Font.BOLD, 14));
                valor.setForeground(Tema.VERDE_HOVER);
                listaUltimasVendas.add(linhaLista(
                        v.quantidade() + "x " + v.nomeProduto(),
                        FORMATO_DATA.format(v.data()), valor));
            }
        }
        listaUltimasVendas.revalidate();
        listaUltimasVendas.repaint();
    }

    // ------------------------------------------------------------------

    private static JLabel valorGrande() {
        JLabel r = new JLabel("—");
        r.setFont(Tema.fonte(Font.BOLD, 21));
        r.setForeground(Tema.TEXTO);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        return r;
    }

    private static JLabel legenda(String texto) {
        JLabel r = new JLabel(texto);
        r.setFont(Tema.fonte(Font.PLAIN, 11));
        r.setForeground(Tema.TEXTO_SUAVE);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        return r;
    }

    private static JComponent cartaoIndicador(String rotulo, JLabel valor,
            JLabel legenda, String icone, Color corIcone, Color fundoIcone) {
        Tema.Cartao cartao = new Tema.Cartao();
        cartao.setLayout(new BorderLayout(12, 0));
        cartao.setBorder(new EmptyBorder(19, 17, 19, 17));

        JPanel bolha = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fundoIcone);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        bolha.setOpaque(false);
        bolha.setPreferredSize(new Dimension(42, 42));
        bolha.add(new JLabel(Tema.icone(icone, 22, corIcone)));
        JPanel colunaIcone = new JPanel(new GridBagLayout());
        colunaIcone.setOpaque(false);
        colunaIcone.add(bolha);
        cartao.add(colunaIcone, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel(rotulo);
        nome.setFont(Tema.fonte(Font.BOLD, 12));
        nome.setForeground(Tema.TEXTO_SUAVE);
        nome.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(Box.createVerticalGlue());
        textos.add(nome);
        textos.add(Box.createVerticalStrut(2));
        textos.add(valor);
        textos.add(Box.createVerticalStrut(2));
        textos.add(legenda);
        textos.add(Box.createVerticalGlue());
        cartao.add(textos, BorderLayout.CENTER);
        return cartao;
    }

    private static JComponent cartaoLista(String titulo, String subtitulo,
                                          JPanel lista) {
        Tema.Cartao cartao = new Tema.Cartao();
        cartao.setLayout(new BorderLayout(0, 12));

        JPanel topo = new JPanel();
        topo.setOpaque(false);
        topo.setLayout(new BoxLayout(topo, BoxLayout.Y_AXIS));
        JLabel rotuloTitulo = new JLabel(titulo);
        rotuloTitulo.setFont(Tema.fonte(Font.BOLD, 16));
        rotuloTitulo.setForeground(Tema.TEXTO);
        JLabel rotuloSub = new JLabel(subtitulo);
        rotuloSub.setFont(Tema.fonte(Font.PLAIN, 12));
        rotuloSub.setForeground(Tema.TEXTO_SUAVE);
        topo.add(rotuloTitulo);
        topo.add(Box.createVerticalStrut(2));
        topo.add(rotuloSub);
        cartao.add(topo, BorderLayout.NORTH);

        lista.setOpaque(false);
        lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
        JPanel envelope = new JPanel(new BorderLayout());
        envelope.setOpaque(false);
        envelope.add(lista, BorderLayout.NORTH);
        JScrollPane rolagem = Tema.rolagemSuave(envelope);
        rolagem.getViewport().setOpaque(false);
        rolagem.setOpaque(false);
        cartao.add(rolagem, BorderLayout.CENTER);
        return cartao;
    }

    private static JComponent linhaLista(String principal, String secundario,
                                         JComponent direita) {
        JPanel linha = new JPanel(new BorderLayout(12, 0));
        linha.setOpaque(false);
        linha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                new EmptyBorder(10, 2, 10, 2)));
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel rotuloPrincipal = new JLabel(principal);
        rotuloPrincipal.setFont(Tema.fonte(Font.BOLD, 14));
        rotuloPrincipal.setForeground(Tema.TEXTO);
        JLabel rotuloSecundario = new JLabel(secundario);
        rotuloSecundario.setFont(Tema.fonte(Font.PLAIN, 12));
        rotuloSecundario.setForeground(Tema.TEXTO_SUAVE);
        textos.add(rotuloPrincipal);
        textos.add(Box.createVerticalStrut(2));
        textos.add(rotuloSecundario);
        linha.add(textos, BorderLayout.CENTER);

        JPanel colunaDireita = new JPanel(new GridBagLayout());
        colunaDireita.setOpaque(false);
        colunaDireita.add(direita);
        linha.add(colunaDireita, BorderLayout.EAST);
        return linha;
    }

    private static JComponent estadoVazio(String icone, Color cor, String texto) {
        JPanel painel = new JPanel();
        painel.setOpaque(false);
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(new EmptyBorder(28, 0, 20, 0));
        JLabel rotuloIcone = new JLabel(Tema.icone(icone, 34, cor));
        rotuloIcone.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel rotuloTexto = new JLabel(texto);
        rotuloTexto.setFont(Tema.fonte(Font.PLAIN, 14));
        rotuloTexto.setForeground(Tema.TEXTO_SUAVE);
        rotuloTexto.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(rotuloIcone);
        painel.add(Box.createVerticalStrut(10));
        painel.add(rotuloTexto);
        return painel;
    }
}

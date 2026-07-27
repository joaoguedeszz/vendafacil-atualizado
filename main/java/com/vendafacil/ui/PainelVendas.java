package com.vendafacil.ui;

import com.vendafacil.dominio.Moeda;
import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.dominio.Venda;
import com.vendafacil.servico.ProdutoServico;
import com.vendafacil.servico.VendaServico;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Histórico de vendas: registro com baixa de estoque, cancelamento com
 * devolução ao estoque e resumo de receita.
 */
public class PainelVendas extends JPanel implements TelaPrincipal.PainelAtualizavel {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProdutoServico produtos;
    private final VendaServico vendas;
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private final JLabel subtitulo = new JLabel();

    /** Vendas exibidas, alinhadas 1:1 com as linhas do modelo. */
    private final List<Venda> linhas = new ArrayList<>();

    public PainelVendas(ProdutoServico produtos, VendaServico vendas) {
        this.produtos = produtos;
        this.vendas = vendas;
        setLayout(new BorderLayout(0, 20));
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        modelo = new DefaultTableModel(
                new Object[]{"Data", "Produto", "Qtd.", "Preço unit.", "Total"}, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        tabela = new JTable(modelo);
        Tema.estilizarTabela(tabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<DefaultTableModel> ordenador = new TableRowSorter<>(modelo);
        ordenador.setComparator(0, Comparator.comparing(v -> (LocalDateTime) v));
        ordenador.setComparator(2, Comparator.comparingInt(v -> (Integer) v));
        ordenador.setComparator(3, Comparator.comparingLong(v -> (Long) v));
        ordenador.setComparator(4, Comparator.comparingLong(v -> (Long) v));
        ordenador.setSortKeys(List.of(
                new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        tabela.setRowSorter(ordenador);

        tabela.getColumnModel().getColumn(0).setCellRenderer(new RendererData());
        tabela.getColumnModel().getColumn(2).setCellRenderer(new RendererCentro());
        tabela.getColumnModel().getColumn(3).setCellRenderer(new RendererMoeda());
        tabela.getColumnModel().getColumn(4).setCellRenderer(new RendererMoeda());
        tabela.getColumnModel().getColumn(0).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(320);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(70);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(110);

        add(criarCabecalho(), BorderLayout.NORTH);

        Tema.Cartao cartao = new Tema.Cartao();
        cartao.setLayout(new BorderLayout());
        cartao.add(Tema.rolagemSuave(tabela), BorderLayout.CENTER);
        add(cartao, BorderLayout.CENTER);
    }

    @Override public JComponent componente() { return this; }

    @Override
    public void atualizar() {
        linhas.clear();
        modelo.setRowCount(0);
        for (Venda v : vendas.listar()) {
            linhas.add(v);
            modelo.addRow(new Object[]{v.data(), v.nomeProduto(),
                    v.quantidade(), v.precoUnitarioCentavos(),
                    v.totalCentavos()});
        }
        int total = linhas.size();
        String contagem = total == 1 ? "1 venda registrada"
                : total + " vendas registradas";
        subtitulo.setText(contagem + "  ·  Receita total: "
                + Moeda.formatar(vendas.receitaTotalCentavos()));
    }

    // ------------------------------------------------------------------

    private JComponent criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Vendas");
        titulo.setFont(Tema.fonte(Font.BOLD, 24));
        titulo.setForeground(Tema.TEXTO);
        subtitulo.setFont(Tema.fonte(Font.PLAIN, 14));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(subtitulo);
        cabecalho.add(titulos, BorderLayout.WEST);

        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        acoes.setOpaque(false);

        JButton btnCancelar = Tema.botaoSecundario("Cancelar venda",
                Tema.icone("lixeira", 18, Tema.PERIGO));
        btnCancelar.setForeground(Tema.PERIGO);
        btnCancelar.addActionListener(e -> cancelarVenda());
        acoes.add(btnCancelar);

        JButton btnNova = Tema.botaoPrimario("Nova venda",
                Tema.icone("mais", 18, Color.WHITE));
        btnNova.addActionListener(e -> {
            if (dialogoNovaVenda(SwingUtilities.getWindowAncestor(this),
                    produtos, vendas, null)) {
                atualizar();
            }
        });
        acoes.add(btnNova);

        cabecalho.add(acoes, BorderLayout.EAST);
        return cabecalho;
    }

    private void cancelarVenda() {
        int linhaVisao = tabela.getSelectedRow();
        if (linhaVisao < 0) {
            Dialogos.informacao(this, "Nenhuma venda selecionada",
                    "Selecione na tabela a venda que deseja cancelar.");
            return;
        }
        Venda v = linhas.get(tabela.convertRowIndexToModel(linhaVisao));
        boolean confirmado = Dialogos.confirmar(this, "Cancelar venda",
                "Cancelar a venda de " + v.quantidade() + "x \""
                        + v.nomeProduto() + "\" ("
                        + Moeda.formatar(v.totalCentavos()) + ")?\n"
                        + "A quantidade será devolvida ao estoque.",
                "Cancelar venda", true);
        if (!confirmado) return;
        boolean estoqueRestaurado;
        try {
            estoqueRestaurado = vendas.cancelar(v.id());
        } catch (RegraDeNegocioException erro) {
            Dialogos.erro(this, "Não foi possível cancelar", erro.getMessage());
            atualizar();
            return;
        }
        atualizar();
        if (!estoqueRestaurado) {
            Dialogos.informacao(this, "Venda cancelada",
                    "A venda foi cancelada, mas o produto já não existe no "
                            + "cadastro — o estoque não foi ajustado.");
        }
    }

    // ------------------------------------------------------------------
    // Diálogo de nova venda (compartilhado com o painel de produtos)
    // ------------------------------------------------------------------

    /**
     * Abre o formulário de nova venda.
     *
     * @param produtoPreSelecionado id do produto a pré-selecionar, ou null.
     * @return true se uma venda foi registrada.
     */
    public static boolean dialogoNovaVenda(Window dono, ProdutoServico produtos,
                                           VendaServico vendas,
                                           Long produtoPreSelecionado) {
        List<Produto> disponiveis = produtos.disponiveisParaVenda();
        if (disponiveis.isEmpty()) {
            Dialogos.erro(dono, "Sem estoque disponível",
                    "Nenhum produto possui estoque para venda.\n"
                            + "Cadastre produtos ou reponha o estoque primeiro.");
            return false;
        }

        JDialog dialogo = new JDialog(dono, "Nova venda",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final boolean[] vendeu = {false};

        JPanel conteudo = new JPanel();
        conteudo.setBackground(Tema.CARTAO);
        conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
        conteudo.setBorder(new EmptyBorder(26, 28, 22, 28));

        JLabel titulo = new JLabel("Nova venda");
        titulo.setFont(Tema.fonte(Font.BOLD, 19));
        titulo.setForeground(Tema.TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        conteudo.add(titulo);
        conteudo.add(Box.createVerticalStrut(20));

        conteudo.add(rotulo("Produto"));
        conteudo.add(Box.createVerticalStrut(6));
        JComboBox<Produto> comboProduto =
                new JComboBox<>(disponiveis.toArray(new Produto[0]));
        comboProduto.setFont(Tema.fonte(Font.PLAIN, 14));
        comboProduto.setBackground(Tema.CARTAO);
        comboProduto.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista,
                    Object valor, int indice, boolean sel, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, sel, foco);
                if (valor instanceof Produto p) {
                    setText(p.nome() + "  ·  " + Moeda.formatar(p.precoCentavos())
                            + "  ·  " + p.quantidade() + " em estoque");
                }
                setBorder(new EmptyBorder(6, 10, 6, 10));
                if (indice == -1 || !sel) {
                    // Valor exibido no combo fechado e itens não selecionados.
                    setBackground(Tema.CARTAO);
                    setForeground(Tema.TEXTO);
                } else {
                    setBackground(Tema.VERDE_CLARO);
                    setForeground(Tema.TEXTO);
                }
                return this;
            }
        });
        conteudo.add(campo(comboProduto));
        conteudo.add(Box.createVerticalStrut(14));

        conteudo.add(rotulo("Quantidade"));
        conteudo.add(Box.createVerticalStrut(6));
        SpinnerNumberModel modeloQtd = new SpinnerNumberModel(1, 1,
                disponiveis.get(0).quantidade(), 1);
        JSpinner campoQtd = new JSpinner(modeloQtd);
        Tema.estilizarSpinner(campoQtd);
        conteudo.add(campo(campoQtd));
        conteudo.add(Box.createVerticalStrut(18));

        JLabel rotuloTotal = new JLabel();
        rotuloTotal.setFont(Tema.fonte(Font.BOLD, 22));
        rotuloTotal.setForeground(Tema.VERDE_HOVER);
        rotuloTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel legendaTotal = new JLabel("Total da venda");
        legendaTotal.setFont(Tema.fonte(Font.PLAIN, 12));
        legendaTotal.setForeground(Tema.TEXTO_SUAVE);
        legendaTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        conteudo.add(legendaTotal);
        conteudo.add(rotuloTotal);
        conteudo.add(Box.createVerticalStrut(6));

        JLabel rotuloErro = new JLabel(" ");
        rotuloErro.setFont(Tema.fonte(Font.PLAIN, 13));
        rotuloErro.setForeground(Tema.PERIGO);
        rotuloErro.setAlignmentX(Component.LEFT_ALIGNMENT);
        conteudo.add(rotuloErro);

        Runnable atualizarTotal = () -> {
            Produto p = (Produto) comboProduto.getSelectedItem();
            int qtd = (Integer) campoQtd.getValue();
            rotuloTotal.setText(p == null ? "—"
                    : Moeda.formatar(p.precoCentavos() * qtd));
        };
        comboProduto.addActionListener(e -> {
            Produto p = (Produto) comboProduto.getSelectedItem();
            if (p != null) {
                modeloQtd.setMaximum(p.quantidade());
                if ((Integer) campoQtd.getValue() > p.quantidade())
                    campoQtd.setValue(p.quantidade());
            }
            atualizarTotal.run();
        });
        campoQtd.addChangeListener(e -> atualizarTotal.run());

        if (produtoPreSelecionado != null) {
            for (Produto p : disponiveis)
                if (p.id() == produtoPreSelecionado)
                    comboProduto.setSelectedItem(p);
        }
        atualizarTotal.run();

        dialogo.add(conteudo, BorderLayout.CENTER);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botoes.setBackground(Tema.CARTAO);
        botoes.setBorder(new EmptyBorder(0, 28, 22, 28));
        JButton cancelar = Tema.botaoSecundario("Cancelar", null);
        cancelar.addActionListener(e -> dialogo.dispose());
        JButton confirmar = Tema.botaoPrimario("Registrar venda",
                Tema.icone("carrinho", 18, Color.WHITE));
        confirmar.addActionListener(e -> {
            Produto p = (Produto) comboProduto.getSelectedItem();
            if (p == null) return;
            try {
                vendas.registrar(p.id(), (Integer) campoQtd.getValue());
                vendeu[0] = true;
                dialogo.dispose();
            } catch (RegraDeNegocioException erro) {
                rotuloErro.setText(erro.getMessage());
            }
        });
        botoes.add(cancelar);
        botoes.add(confirmar);
        dialogo.add(botoes, BorderLayout.SOUTH);
        dialogo.getRootPane().setDefaultButton(confirmar);

        dialogo.getContentPane().setBackground(Tema.CARTAO);
        dialogo.pack();
        dialogo.setSize(Math.max(dialogo.getWidth(), 460), dialogo.getHeight());
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(dono);
        dialogo.setVisible(true);
        return vendeu[0];
    }

    private static JLabel rotulo(String texto) {
        JLabel r = new JLabel(texto);
        r.setFont(Tema.fonte(Font.BOLD, 13));
        r.setForeground(Tema.TEXTO);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        return r;
    }

    private static JComponent campo(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        c.setPreferredSize(new Dimension(200, 44));
        return c;
    }

    // ------------------------------------------------------------------
    // Renderers
    // ------------------------------------------------------------------

    private static class RendererData extends Tema.RendererLinha {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foco, int lin, int col) {
            super.getTableCellRendererComponent(t,
                    FORMATO_DATA.format((LocalDateTime) v), sel, foco, lin, col);
            return this;
        }
    }

    private static class RendererMoeda extends Tema.RendererLinha {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foco, int lin, int col) {
            super.getTableCellRendererComponent(t, Moeda.formatar((Long) v),
                    sel, foco, lin, col);
            setHorizontalAlignment(RIGHT);
            return this;
        }
    }

    private static class RendererCentro extends Tema.RendererLinha {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foco, int lin, int col) {
            super.getTableCellRendererComponent(t, v, sel, foco, lin, col);
            setHorizontalAlignment(CENTER);
            return this;
        }
    }
}

package com.vendafacil.ui;

import com.vendafacil.dominio.Moeda;
import com.vendafacil.dominio.Produto;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.dominio.SituacaoEstoque;
import com.vendafacil.servico.ProdutoServico;
import com.vendafacil.servico.VendaServico;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Cadastro de produtos: busca ao vivo, ordenação por coluna,
 * criação/edição em formulário próprio, exclusão com confirmação
 * e venda rápida do produto selecionado.
 */
public class PainelProdutos extends JPanel implements TelaPrincipal.PainelAtualizavel {

    private final ProdutoServico produtos;
    private final VendaServico vendas;
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private final TableRowSorter<DefaultTableModel> ordenador;
    private final Tema.CampoTexto campoBusca = new Tema.CampoTexto("Buscar produto…");
    private final JLabel subtitulo = new JLabel();

    /** Produtos exibidos, alinhados 1:1 com as linhas do modelo. */
    private final List<Produto> linhas = new ArrayList<>();

    public PainelProdutos(ProdutoServico produtos, VendaServico vendas) {
        this.produtos = produtos;
        this.vendas = vendas;
        setLayout(new BorderLayout(0, 20));
        setBackground(Tema.FUNDO);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        modelo = new DefaultTableModel(
                new Object[]{"Produto", "Preço", "Estoque", "Situação"}, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) {
                return false; // tabela somente leitura: edição só pelo formulário
            }
        };
        tabela = new JTable(modelo);
        Tema.estilizarTabela(tabela);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ordenador = new TableRowSorter<>(modelo);
        ordenador.setComparator(1, Comparator.comparingLong(v -> (Long) v));
        ordenador.setComparator(2, Comparator.comparingInt(v -> (Integer) v));
        ordenador.setComparator(3, Comparator.comparingInt(v -> (Integer) v));
        tabela.setRowSorter(ordenador);

        tabela.getColumnModel().getColumn(1).setCellRenderer(new RendererPreco());
        tabela.getColumnModel().getColumn(2).setCellRenderer(new RendererCentralizado());
        tabela.getColumnModel().getColumn(3).setCellRenderer(new RendererSituacao());
        tabela.getColumnModel().getColumn(0).setPreferredWidth(340);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(120);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(90);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(140);

        tabela.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && produtoSelecionado() != null) editar();
            }
        });

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
        for (Produto p : produtos.listar()) {
            linhas.add(p);
            modelo.addRow(new Object[]{p.nome(), p.precoCentavos(),
                    p.quantidade(), p.quantidade()});
        }
        int total = linhas.size();
        subtitulo.setText(total == 1 ? "1 produto cadastrado"
                : total + " produtos cadastrados");
    }

    // ------------------------------------------------------------------

    private JComponent criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout());
        cabecalho.setOpaque(false);

        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Produtos");
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

        campoBusca.setPreferredSize(new Dimension(230, 44));
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        acoes.add(campoBusca);

        JButton btnVender = Tema.botaoAzul("Vender",
                Tema.icone("carrinho", 18, Color.WHITE));
        btnVender.addActionListener(e -> vender());
        acoes.add(btnVender);

        JButton btnNovo = Tema.botaoPrimario("Novo produto",
                Tema.icone("mais", 18, Color.WHITE));
        btnNovo.addActionListener(e -> {
            if (new DialogoProduto(janela(), produtos, null).exibir()) atualizar();
        });
        acoes.add(btnNovo);

        JButton btnEditar = Tema.botaoSecundario("Editar",
                Tema.icone("lapis", 18, Tema.TEXTO));
        btnEditar.addActionListener(e -> editar());
        acoes.add(btnEditar);

        JButton btnExcluir = Tema.botaoSecundario("Excluir",
                Tema.icone("lixeira", 18, Tema.PERIGO));
        btnExcluir.setForeground(Tema.PERIGO);
        btnExcluir.addActionListener(e -> excluir());
        acoes.add(btnExcluir);

        cabecalho.add(acoes, BorderLayout.EAST);
        return cabecalho;
    }

    private void filtrar() {
        String texto = campoBusca.getText().trim();
        if (texto.isEmpty()) {
            ordenador.setRowFilter(null);
            return;
        }
        String busca = texto.toLowerCase(Locale.ROOT);
        ordenador.setRowFilter(new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel,
                    ? extends Integer> entrada) {
                return entrada.getStringValue(0).toLowerCase(Locale.ROOT).contains(busca);
            }
        });
    }

    /**
     * Produto da linha selecionada. Converte o índice da visão (que muda com
     * busca e ordenação) para o índice real do modelo — no app antigo essa
     * conversão não existia e a ação atingia o produto errado.
     */
    private Produto produtoSelecionado() {
        int linhaVisao = tabela.getSelectedRow();
        if (linhaVisao < 0) return null;
        return linhas.get(tabela.convertRowIndexToModel(linhaVisao));
    }

    private Window janela() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void editar() {
        Produto p = produtoSelecionado();
        if (p == null) {
            Dialogos.informacao(this, "Nenhum produto selecionado",
                    "Selecione na tabela o produto que deseja editar.");
            return;
        }
        if (new DialogoProduto(janela(), produtos, p).exibir()) atualizar();
    }

    private void excluir() {
        Produto p = produtoSelecionado();
        if (p == null) {
            Dialogos.informacao(this, "Nenhum produto selecionado",
                    "Selecione na tabela o produto que deseja excluir.");
            return;
        }
        boolean confirmado = Dialogos.confirmar(this, "Excluir produto",
                "Excluir \"" + p.nome() + "\" do estoque?\n"
                        + "O histórico de vendas do produto será preservado.",
                "Excluir", true);
        if (!confirmado) return;
        try {
            produtos.excluir(p.id());
        } catch (RegraDeNegocioException erro) {
            Dialogos.erro(this, "Não foi possível excluir", erro.getMessage());
        }
        atualizar();
    }

    private void vender() {
        Produto p = produtoSelecionado();
        if (PainelVendas.dialogoNovaVenda(janela(), produtos, vendas,
                p == null ? null : p.id())) {
            atualizar();
        }
    }

    // ------------------------------------------------------------------
    // Renderers
    // ------------------------------------------------------------------

    private static class RendererPreco extends Tema.RendererLinha {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foco, int lin, int col) {
            super.getTableCellRendererComponent(t, Moeda.formatar((Long) v),
                    sel, foco, lin, col);
            setHorizontalAlignment(RIGHT);
            return this;
        }
    }

    private static class RendererCentralizado extends Tema.RendererLinha {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foco, int lin, int col) {
            super.getTableCellRendererComponent(t, v, sel, foco, lin, col);
            setHorizontalAlignment(CENTER);
            return this;
        }
    }

    private static class RendererSituacao implements javax.swing.table.TableCellRenderer {
        private final JPanel painel = new JPanel(new GridBagLayout());
        private final Tema.EtiquetaPill pill = new Tema.EtiquetaPill();

        RendererSituacao() {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 14, 0, 0);
            painel.add(pill, gbc);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foco, int lin, int col) {
            SituacaoEstoque situacao = SituacaoEstoque.de((Integer) v);
            switch (situacao) {
                case ESGOTADO -> pill.configurar(situacao.rotulo(),
                        Tema.PERIGO_CLARO, Tema.PERIGO);
                case BAIXO -> pill.configurar(situacao.rotulo(),
                        Tema.ALERTA_CLARO, Tema.ALERTA);
                case NORMAL -> pill.configurar(situacao.rotulo(),
                        Tema.VERDE_CLARO, Tema.VERDE_HOVER);
            }
            painel.setBackground(sel ? t.getSelectionBackground()
                    : lin % 2 == 0 ? Tema.CARTAO : Tema.LINHA_ZEBRA);
            return painel;
        }
    }

    // ------------------------------------------------------------------
    // Formulário de produto (novo/editar)
    // ------------------------------------------------------------------

    private static class DialogoProduto extends JDialog {
        private final ProdutoServico servico;
        private final Produto existente;
        private final Tema.CampoTexto campoNome = new Tema.CampoTexto("Ex.: Café 500g");
        private final Tema.CampoTexto campoPreco = new Tema.CampoTexto("Ex.: 12,50");
        private final JSpinner campoQuantidade =
                new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));
        private final JLabel rotuloErro = new JLabel(" ");
        private boolean salvo;

        DialogoProduto(Window dono, ProdutoServico servico, Produto existente) {
            super(dono, existente == null ? "Novo produto" : "Editar produto",
                    ModalityType.APPLICATION_MODAL);
            this.servico = servico;
            this.existente = existente;
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel conteudo = new JPanel();
            conteudo.setBackground(Tema.CARTAO);
            conteudo.setLayout(new BoxLayout(conteudo, BoxLayout.Y_AXIS));
            conteudo.setBorder(new EmptyBorder(26, 28, 22, 28));

            JLabel titulo = new JLabel(getTitle());
            titulo.setFont(Tema.fonte(Font.BOLD, 19));
            titulo.setForeground(Tema.TEXTO);
            titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
            conteudo.add(titulo);
            conteudo.add(Box.createVerticalStrut(20));

            conteudo.add(rotulo("Nome do produto"));
            conteudo.add(Box.createVerticalStrut(6));
            conteudo.add(campo(campoNome));
            conteudo.add(Box.createVerticalStrut(14));

            conteudo.add(rotulo("Preço unitário (R$)"));
            conteudo.add(Box.createVerticalStrut(6));
            conteudo.add(campo(campoPreco));
            conteudo.add(Box.createVerticalStrut(14));

            conteudo.add(rotulo("Quantidade em estoque"));
            conteudo.add(Box.createVerticalStrut(6));
            Tema.estilizarSpinner(campoQuantidade);
            conteudo.add(campo(campoQuantidade));
            conteudo.add(Box.createVerticalStrut(10));

            rotuloErro.setFont(Tema.fonte(Font.PLAIN, 13));
            rotuloErro.setForeground(Tema.PERIGO);
            rotuloErro.setAlignmentX(Component.LEFT_ALIGNMENT);
            conteudo.add(rotuloErro);
            add(conteudo, BorderLayout.CENTER);

            JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            botoes.setBackground(Tema.CARTAO);
            botoes.setBorder(new EmptyBorder(0, 28, 22, 28));
            JButton cancelar = Tema.botaoSecundario("Cancelar", null);
            cancelar.addActionListener(e -> dispose());
            JButton salvar = Tema.botaoPrimario(
                    existente == null ? "Cadastrar" : "Salvar alterações", null);
            salvar.addActionListener(e -> salvar());
            botoes.add(cancelar);
            botoes.add(salvar);
            add(botoes, BorderLayout.SOUTH);
            getRootPane().setDefaultButton(salvar);

            if (existente != null) {
                campoNome.setText(existente.nome());
                campoPreco.setText(Moeda.paraCampo(existente.precoCentavos()));
                campoQuantidade.setValue(existente.quantidade());
            }

            getContentPane().setBackground(Tema.CARTAO);
            pack();
            setSize(Math.max(getWidth(), 420), getHeight());
            setResizable(false);
            setLocationRelativeTo(dono);
        }

        boolean exibir() {
            setVisible(true);
            return salvo;
        }

        private void salvar() {
            try {
                String nome = campoNome.getText();
                long preco = Moeda.parseCentavos(campoPreco.getText());
                int quantidade = (Integer) campoQuantidade.getValue();
                if (existente == null)
                    servico.cadastrar(nome, preco, quantidade);
                else
                    servico.atualizar(existente.id(), nome, preco, quantidade);
                salvo = true;
                dispose();
            } catch (RegraDeNegocioException erro) {
                rotuloErro.setText(erro.getMessage());
            }
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
    }
}

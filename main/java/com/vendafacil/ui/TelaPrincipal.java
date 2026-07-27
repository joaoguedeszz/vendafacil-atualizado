package com.vendafacil.ui;

import com.vendafacil.app.Contexto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** Janela principal: barra lateral de navegação e painéis em CardLayout. */
public class TelaPrincipal extends JFrame {

    private final Contexto contexto;
    private final CardLayout cartoes = new CardLayout();
    private final JPanel conteudo = new JPanel(cartoes);
    private final Map<String, BotaoNavegacao> botoesNav = new LinkedHashMap<>();
    private final Map<String, PainelAtualizavel> paineis = new LinkedHashMap<>();

    /** Painel de conteúdo que sabe se atualizar quando exibido. */
    public interface PainelAtualizavel {
        void atualizar();
        JComponent componente();
    }

    public TelaPrincipal(Contexto contexto, String usuario) {
        super("VendaFácil — Sistema de Estoque e Vendas");
        this.contexto = contexto;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1220, 760);
        setMinimumSize(new Dimension(1020, 640));
        setLocationRelativeTo(null);

        conteudo.setBackground(Tema.FUNDO);

        // Cada painel recebe apenas os serviços de que precisa.
        registrarPainel("dashboard", new PainelDashboard(contexto.relatorios()));
        registrarPainel("produtos",
                new PainelProdutos(contexto.produtos(), contexto.vendas()));
        registrarPainel("vendas",
                new PainelVendas(contexto.produtos(), contexto.vendas()));

        add(criarSidebar(usuario), BorderLayout.WEST);
        add(conteudo, BorderLayout.CENTER);

        trocarTela("dashboard");
    }

    private void registrarPainel(String id, PainelAtualizavel painel) {
        paineis.put(id, painel);
        conteudo.add(painel.componente(), id);
    }

    public void trocarTela(String id) {
        paineis.get(id).atualizar();
        cartoes.show(conteudo, id);
        botoesNav.forEach((chave, botao) -> botao.setAtivo(chave.equals(id)));
    }

    // ------------------------------------------------------------------
    // Sidebar
    // ------------------------------------------------------------------

    private JComponent criarSidebar(String usuario) {
        JPanel barra = new JPanel();
        barra.setBackground(Tema.SIDEBAR);
        barra.setPreferredSize(new Dimension(236, 0));
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBorder(new EmptyBorder(24, 16, 20, 16));

        barra.add(criarMarca());
        barra.add(Box.createVerticalStrut(32));

        JLabel menu = new JLabel("MENU");
        menu.setFont(Tema.fonte(Font.BOLD, 11));
        menu.setForeground(new Color(0x64748B));
        menu.setBorder(new EmptyBorder(0, 12, 8, 0));
        menu.setAlignmentX(Component.LEFT_ALIGNMENT);
        barra.add(menu);

        barra.add(criarBotaoNav("dashboard", "Dashboard", "dashboard"));
        barra.add(Box.createVerticalStrut(6));
        barra.add(criarBotaoNav("produtos", "Produtos", "caixa"));
        barra.add(Box.createVerticalStrut(6));
        barra.add(criarBotaoNav("vendas", "Vendas", "carrinho"));

        barra.add(Box.createVerticalGlue());

        JPanel usuarioLinha = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        usuarioLinha.setOpaque(false);
        usuarioLinha.setAlignmentX(Component.LEFT_ALIGNMENT);
        usuarioLinha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        usuarioLinha.add(new JLabel(Tema.icone("usuario", 20, Tema.SIDEBAR_TEXTO)));
        JLabel nomeUsuario = new JLabel(usuario);
        nomeUsuario.setFont(Tema.fonte(Font.BOLD, 14));
        nomeUsuario.setForeground(Color.WHITE);
        usuarioLinha.add(nomeUsuario);
        barra.add(usuarioLinha);
        barra.add(Box.createVerticalStrut(10));

        BotaoNavegacao sair = new BotaoNavegacao("Sair",
                Tema.icone("sair", 20, Tema.SIDEBAR_TEXTO),
                Tema.icone("sair", 20, Color.WHITE));
        sair.addActionListener(e -> {
            if (Dialogos.confirmar(this, "Sair do sistema",
                    "Deseja realmente encerrar a sessão?", "Sair", false)) {
                dispose();
                new TelaLogin(contexto).setVisible(true);
            }
        });
        barra.add(sair);
        return barra;
    }

    private JComponent criarMarca() {
        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        marca.setOpaque(false);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel emblema = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x22C55E),
                        getWidth(), getHeight(), new Color(0x2173C4)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        emblema.setOpaque(false);
        emblema.setPreferredSize(new Dimension(38, 38));
        emblema.add(new JLabel(Tema.icone("carrinho", 22, Color.WHITE)));
        marca.add(emblema);

        JLabel nome = new JLabel("<html><span color='#FFFFFF'>Venda</span>"
                + "<span color='#4ADE80'>Fácil</span></html>");
        nome.setFont(Tema.fonte(Font.BOLD, 19));
        marca.add(nome);
        return marca;
    }

    private BotaoNavegacao criarBotaoNav(String id, String rotulo, String icone) {
        BotaoNavegacao botao = new BotaoNavegacao(rotulo,
                Tema.icone(icone, 20, Tema.SIDEBAR_TEXTO),
                Tema.icone(icone, 20, Color.WHITE));
        botao.addActionListener(e -> trocarTela(id));
        botoesNav.put(id, botao);
        return botao;
    }

    /** Botão da barra lateral com estados normal/hover/ativo. */
    private static class BotaoNavegacao extends JButton {
        private final Icon iconeNormal;
        private final Icon iconeAtivo;
        private boolean ativo;

        BotaoNavegacao(String rotulo, Icon iconeNormal, Icon iconeAtivo) {
            super(rotulo, iconeNormal);
            this.iconeNormal = iconeNormal;
            this.iconeAtivo = iconeAtivo;
            setHorizontalAlignment(LEFT);
            setIconTextGap(12);
            setFont(Tema.fonte(Font.BOLD, 14));
            setForeground(Tema.SIDEBAR_TEXTO);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(new EmptyBorder(11, 14, 11, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setRolloverEnabled(true);
        }

        void setAtivo(boolean ativo) {
            this.ativo = ativo;
            setIcon(ativo ? iconeAtivo : iconeNormal);
            setForeground(ativo ? Color.WHITE : Tema.SIDEBAR_TEXTO);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (ativo) {
                g2.setColor(Tema.VERDE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            } else if (getModel().isRollover() || getModel().isPressed()) {
                g2.setColor(Tema.SIDEBAR_HOVER);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }
}

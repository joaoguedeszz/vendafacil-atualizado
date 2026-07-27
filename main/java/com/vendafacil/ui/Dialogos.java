package com.vendafacil.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/** Diálogos modais (confirmação, informação e erro) no estilo do app. */
public final class Dialogos {

    private Dialogos() {}

    public static boolean confirmar(Component pai, String titulo, String mensagem,
                                    String rotuloConfirmar, boolean perigoso) {
        final boolean[] resposta = {false};
        JDialog dialogo = criarBase(pai, titulo, mensagem,
                perigoso ? Tema.icone("alerta", 26, Tema.PERIGO)
                         : Tema.icone("alerta", 26, Tema.ALERTA),
                perigoso ? Tema.PERIGO_CLARO : Tema.ALERTA_CLARO);

        JButton cancelar = Tema.botaoSecundario("Cancelar", null);
        cancelar.addActionListener(e -> dialogo.dispose());
        JButton confirmar = perigoso
                ? Tema.botaoPerigo(rotuloConfirmar, null)
                : Tema.botaoPrimario(rotuloConfirmar, null);
        confirmar.addActionListener(e -> { resposta[0] = true; dialogo.dispose(); });

        adicionarBotoes(dialogo, confirmar, cancelar, confirmar);
        dialogo.setVisible(true);
        return resposta[0];
    }

    public static void informacao(Component pai, String titulo, String mensagem) {
        JDialog dialogo = criarBase(pai, titulo, mensagem,
                Tema.icone("check", 26, Tema.VERDE), Tema.VERDE_CLARO);
        JButton ok = Tema.botaoPrimario("OK", null);
        ok.addActionListener(e -> dialogo.dispose());
        adicionarBotoes(dialogo, ok, null, ok);
        dialogo.setVisible(true);
    }

    public static void erro(Component pai, String titulo, String mensagem) {
        JDialog dialogo = criarBase(pai, titulo, mensagem,
                Tema.icone("alerta", 26, Tema.PERIGO), Tema.PERIGO_CLARO);
        JButton ok = Tema.botaoPerigo("OK", null);
        ok.addActionListener(e -> dialogo.dispose());
        adicionarBotoes(dialogo, ok, null, ok);
        dialogo.setVisible(true);
    }

    // ------------------------------------------------------------------

    private static JDialog criarBase(Component pai, String titulo, String mensagem,
                                     Icon icone, Color fundoIcone) {
        Window janela = null;
        if (pai instanceof Window w) janela = w;
        else if (pai != null) janela = SwingUtilities.getWindowAncestor(pai);
        JDialog dialogo = new JDialog(janela, titulo,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel conteudo = new JPanel(new BorderLayout(16, 0));
        conteudo.setBackground(Tema.CARTAO);
        conteudo.setBorder(new EmptyBorder(24, 24, 20, 24));

        JPanel bolha = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fundoIcone);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        bolha.setOpaque(false);
        bolha.setPreferredSize(new Dimension(48, 48));
        bolha.add(new JLabel(icone));
        JPanel colunaIcone = new JPanel(new BorderLayout());
        colunaIcone.setOpaque(false);
        colunaIcone.add(bolha, BorderLayout.NORTH);
        conteudo.add(colunaIcone, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(Tema.fonte(Font.BOLD, 16));
        lblTitulo.setForeground(Tema.TEXTO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblMensagem = new JLabel("<html><div style='width:300px'>"
                + mensagem.replace("\n", "<br>") + "</div></html>");
        lblMensagem.setFont(Tema.fonte(Font.PLAIN, 14));
        lblMensagem.setForeground(Tema.TEXTO_SUAVE);
        lblMensagem.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(6));
        textos.add(lblMensagem);
        conteudo.add(textos, BorderLayout.CENTER);

        dialogo.getContentPane().setBackground(Tema.CARTAO);
        dialogo.add(conteudo, BorderLayout.CENTER);

        // ESC fecha o diálogo.
        dialogo.getRootPane().registerKeyboardAction(e -> dialogo.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        return dialogo;
    }

    private static void adicionarBotoes(JDialog dialogo, JButton principal,
                                        JButton secundario, JButton padrao) {
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        botoes.setBackground(Tema.CARTAO);
        botoes.setBorder(new EmptyBorder(0, 24, 20, 24));
        if (secundario != null) botoes.add(secundario);
        botoes.add(principal);
        dialogo.add(botoes, BorderLayout.SOUTH);
        dialogo.getRootPane().setDefaultButton(padrao);
        dialogo.pack();
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(dialogo.getOwner());
    }
}

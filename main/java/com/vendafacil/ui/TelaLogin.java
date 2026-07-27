package com.vendafacil.ui;

import com.vendafacil.app.Contexto;
import com.vendafacil.servico.AutenticacaoServico;
import com.vendafacil.servico.ImportadorLegado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Tela de login.
 *
 * <p>Acesso padrão: {@code admin} / {@code 1234}, criado no banco na primeira
 * execução. A conferência da senha (PBKDF2) roda fora da Event Dispatch
 * Thread — são ~100 ms de propósito, e travar a EDT congelaria a janela.
 */
public class TelaLogin extends JFrame {

    /** O aviso de importação é dado uma vez por execução, não a cada login. */
    private static boolean avisoImportacaoExibido;

    private final Contexto contexto;
    private final Tema.CampoTexto campoUsuario = new Tema.CampoTexto("Digite seu usuário");
    private final Tema.CampoSenha campoSenha = new Tema.CampoSenha("Digite sua senha");
    private final JLabel rotuloErro = new JLabel(" ");
    private final JButton btnEntrar = Tema.botaoPrimario("Entrar", null);

    public TelaLogin(Contexto contexto) {
        super("VendaFácil — Login");
        this.contexto = contexto;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 640);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel fundo = new PainelFundo();
        fundo.setLayout(new GridBagLayout());
        setContentPane(fundo);

        Tema.Cartao cartao = new Tema.Cartao();
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.setBorder(new EmptyBorder(38, 38, 34, 38));

        cartao.add(centralizar(Tema.rotuloLogo(46, false)));
        cartao.add(Box.createVerticalStrut(10));

        JLabel subtitulo = new JLabel("Sistema de estoque e vendas");
        subtitulo.setFont(Tema.fonte(Font.PLAIN, 14));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        cartao.add(centralizar(subtitulo));
        cartao.add(Box.createVerticalStrut(28));

        cartao.add(rotuloCampo("Usuário"));
        cartao.add(Box.createVerticalStrut(6));
        cartao.add(limitarAltura(campoUsuario));
        cartao.add(Box.createVerticalStrut(16));

        cartao.add(rotuloCampo("Senha"));
        cartao.add(Box.createVerticalStrut(6));
        cartao.add(limitarAltura(campoSenha));
        cartao.add(Box.createVerticalStrut(8));

        char ocultador = campoSenha.getEchoChar();
        JCheckBox mostrarSenha = new JCheckBox("Mostrar senha");
        mostrarSenha.setFont(Tema.fonte(Font.PLAIN, 13));
        mostrarSenha.setForeground(Tema.TEXTO_SUAVE);
        mostrarSenha.setOpaque(false);
        mostrarSenha.setFocusPainted(false);
        mostrarSenha.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mostrarSenha.setAlignmentX(Component.LEFT_ALIGNMENT);
        mostrarSenha.addActionListener(e ->
                campoSenha.setEchoChar(mostrarSenha.isSelected() ? (char) 0 : ocultador));
        cartao.add(mostrarSenha);

        rotuloErro.setFont(Tema.fonte(Font.PLAIN, 13));
        rotuloErro.setForeground(Tema.PERIGO);
        rotuloErro.setAlignmentX(Component.LEFT_ALIGNMENT);
        cartao.add(Box.createVerticalStrut(6));
        cartao.add(rotuloErro);
        cartao.add(Box.createVerticalStrut(10));

        btnEntrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnEntrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnEntrar.addActionListener(e -> autenticar());
        cartao.add(limitarAltura(btnEntrar));
        cartao.add(Box.createVerticalStrut(18));

        JLabel dica = new JLabel("Acesso padrão: "
                + AutenticacaoServico.LOGIN_PADRAO + " / "
                + new String(AutenticacaoServico.SENHA_PADRAO));
        dica.setFont(Tema.fonte(Font.PLAIN, 12));
        dica.setForeground(Tema.TEXTO_SUAVE);
        cartao.add(centralizar(dica));

        GridBagConstraints gbc = new GridBagConstraints();
        cartao.setPreferredSize(new Dimension(400, cartao.getPreferredSize().height));
        fundo.add(cartao, gbc);

        // Enter em qualquer campo aciona o login.
        getRootPane().setDefaultButton(btnEntrar);
    }

    private void autenticar() {
        String usuario = campoUsuario.getText().trim();
        char[] senha = campoSenha.getPassword();
        rotuloErro.setText(" ");
        definirOcupado(true);

        // O hash da senha é lento de propósito; fora da EDT a janela continua viva.
        new SwingWorker<Optional<String>, Void>() {
            @Override
            protected Optional<String> doInBackground() {
                return contexto.autenticacao().autenticar(usuario, senha);
            }

            @Override
            protected void done() {
                definirOcupado(false);
                try {
                    get().ifPresentOrElse(TelaLogin.this::abrirPrincipal,
                            TelaLogin.this::mostrarCredenciaisInvalidas);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Dialogos.erro(TelaLogin.this, "Falha ao entrar",
                            "Não foi possível verificar suas credenciais.\n"
                                    + mensagemDe(e.getCause()));
                }
            }
        }.execute();
    }

    private void definirOcupado(boolean ocupado) {
        btnEntrar.setEnabled(!ocupado);
        btnEntrar.setText(ocupado ? "Entrando…" : "Entrar");
        setCursor(Cursor.getPredefinedCursor(
                ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void mostrarCredenciaisInvalidas() {
        rotuloErro.setText("Usuário ou senha incorretos.");
        campoSenha.setText("");
        campoSenha.requestFocusInWindow();
    }

    private void abrirPrincipal(String usuario) {
        TelaPrincipal principal = new TelaPrincipal(contexto, usuario);
        dispose();
        principal.setVisible(true);
        avisarImportacao(principal);
    }

    /** Conta o que foi recuperado do arquivo da versão anterior, se houve. */
    private void avisarImportacao(Component pai) {
        ImportadorLegado.Resultado importacao = contexto.importacaoLegado();
        if (avisoImportacaoExibido || importacao == null || !importacao.executada()) return;
        avisoImportacaoExibido = true;
        Dialogos.informacao(pai, "Dados importados",
                "Os dados da versão anterior foram transferidos para o novo banco:\n"
                        + importacao.produtos() + " produto(s) e "
                        + importacao.vendas() + " venda(s).\n"
                        + "O arquivo antigo foi mantido como cópia de segurança.");
    }

    private static String mensagemDe(Throwable causa) {
        if (causa == null) return "Erro desconhecido.";
        return causa.getMessage() == null ? causa.toString() : causa.getMessage();
    }

    private static JLabel rotuloCampo(String texto) {
        JLabel r = new JLabel(texto);
        r.setFont(Tema.fonte(Font.BOLD, 13));
        r.setForeground(Tema.TEXTO);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        return r;
    }

    /**
     * Envolve o componente num painel de largura total que o centraliza —
     * misturar alignmentX diferentes no BoxLayout desloca o conteúdo.
     */
    private static JComponent centralizar(JComponent c) {
        JPanel envelope = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        envelope.setOpaque(false);
        envelope.setAlignmentX(Component.LEFT_ALIGNMENT);
        envelope.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                c.getPreferredSize().height));
        envelope.add(c);
        return envelope;
    }

    private static JComponent limitarAltura(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, 46));
        return c;
    }

    /** Fundo em degradê escuro com círculos decorativos nas cores da marca. */
    private static class PainelFundo extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(0x0B1626),
                    getWidth(), getHeight(), new Color(0x123B2E)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(34, 197, 94, 26));
            g2.fillOval(-120, getHeight() - 260, 380, 380);
            g2.setColor(new Color(59, 130, 246, 22));
            g2.fillOval(getWidth() - 220, -140, 360, 360);
            g2.setColor(new Color(34, 197, 94, 14));
            g2.fillOval(getWidth() - 130, getHeight() - 180, 260, 260);
            g2.dispose();
        }
    }
}

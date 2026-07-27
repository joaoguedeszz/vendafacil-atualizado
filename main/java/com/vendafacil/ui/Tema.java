package com.vendafacil.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Design system do VendaFácil: paleta de cores (derivada da logo verde/azul),
 * tipografia, botões, campos, cartões, tabelas, ícones vetoriais e scrollbars.
 */
public final class Tema {

    // ------------------------------------------------------------------
    // Paleta
    // ------------------------------------------------------------------
    public static final Color VERDE          = new Color(0x16A34A);
    public static final Color VERDE_HOVER    = new Color(0x15803D);
    public static final Color VERDE_PRESSION = new Color(0x166534);
    public static final Color VERDE_CLARO    = new Color(0xDCFCE7);
    public static final Color AZUL           = new Color(0x2173C4);
    public static final Color AZUL_HOVER     = new Color(0x185EA5);
    public static final Color AZUL_CLARO     = new Color(0xDBEAFE);
    public static final Color PERIGO         = new Color(0xDC2626);
    public static final Color PERIGO_HOVER   = new Color(0xB91C1C);
    public static final Color PERIGO_CLARO   = new Color(0xFEE2E2);
    public static final Color ALERTA         = new Color(0xD97706);
    public static final Color ALERTA_CLARO   = new Color(0xFEF3C7);

    public static final Color FUNDO          = new Color(0xF1F5F9);
    public static final Color CARTAO         = Color.WHITE;
    public static final Color BORDA          = new Color(0xE2E8F0);
    public static final Color LINHA_ZEBRA    = new Color(0xF8FAFC);

    public static final Color TEXTO          = new Color(0x0F172A);
    public static final Color TEXTO_SUAVE    = new Color(0x64748B);

    public static final Color SIDEBAR        = new Color(0x0F172A);
    public static final Color SIDEBAR_HOVER  = new Color(0x1E293B);
    public static final Color SIDEBAR_TEXTO  = new Color(0xCBD5E1);

    public static final int ARCO = 14;

    private static String familiaFonte;

    private Tema() {}

    // ------------------------------------------------------------------
    // Tipografia
    // ------------------------------------------------------------------

    private static String familiaFonte() {
        if (familiaFonte == null) {
            Set<String> instaladas = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getAvailableFontFamilyNames()));
            for (String f : new String[]{"Segoe UI", "Inter", "SF Pro Text",
                    "Helvetica Neue", "Ubuntu", "DejaVu Sans"}) {
                if (instaladas.contains(f)) { familiaFonte = f; break; }
            }
            if (familiaFonte == null) familiaFonte = Font.SANS_SERIF;
        }
        return familiaFonte;
    }

    public static Font fonte(int estilo, int tamanho) {
        return new Font(familiaFonte(), estilo, tamanho);
    }

    /** Ajustes globais de LookAndFeel — chamar uma vez, antes de criar janelas. */
    public static void aplicarAjustesGlobais() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignorada) {}
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        FontUIResource padrao = new FontUIResource(fonte(Font.PLAIN, 14));
        Enumeration<Object> chaves = UIManager.getDefaults().keys();
        while (chaves.hasMoreElements()) {
            Object chave = chaves.nextElement();
            if (UIManager.get(chave) instanceof FontUIResource)
                UIManager.put(chave, padrao);
        }
        UIManager.put("Panel.background", FUNDO);
        UIManager.put("OptionPane.background", CARTAO);
        UIManager.put("ComboBox.background", CARTAO);
        UIManager.put("ComboBox.foreground", TEXTO);
        UIManager.put("ComboBox.selectionBackground", VERDE_CLARO);
        UIManager.put("ComboBox.selectionForeground", TEXTO);
        UIManager.put("ComboBox.buttonBackground", CARTAO);
        UIManager.put("Spinner.background", CARTAO);
        UIManager.put("FormattedTextField.background", CARTAO);
        UIManager.put("ToolTip.background", SIDEBAR);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.border", new EmptyBorder(6, 10, 6, 10));
    }

    // ------------------------------------------------------------------
    // Logo
    // ------------------------------------------------------------------

    /**
     * Carrega a logo (classpath ou arquivo), recorta as margens transparentes
     * e redimensiona para a altura pedida; null se o arquivo não existir.
     */
    public static ImageIcon carregarLogo(int altura) {
        java.awt.image.BufferedImage imagem = null;
        try {
            java.net.URL url = Tema.class.getResource("/logo.png");
            if (url != null) {
                imagem = javax.imageio.ImageIO.read(url);
            } else if (Files.exists(Path.of("logo.png"))) {
                imagem = javax.imageio.ImageIO.read(Path.of("logo.png").toFile());
            }
        } catch (java.io.IOException e) {
            return null;
        }
        if (imagem == null) return null;
        imagem = recortarTransparencia(imagem);
        int largura = Math.max(1, imagem.getWidth() * altura / imagem.getHeight());
        return new ImageIcon(imagem.getScaledInstance(largura, altura,
                Image.SCALE_SMOOTH));
    }

    /** Caixa mínima dos pixels visíveis (alfa relevante) da imagem. */
    private static java.awt.image.BufferedImage recortarTransparencia(
            java.awt.image.BufferedImage imagem) {
        int minX = imagem.getWidth(), minY = imagem.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                if (((imagem.getRGB(x, y) >>> 24) & 0xFF) > 16) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < minX) return imagem;
        return imagem.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /** Rótulo com a logo ou, na ausência do arquivo, o nome estilizado. */
    public static JLabel rotuloLogo(int altura, boolean fundoEscuro) {
        ImageIcon logo = carregarLogo(altura);
        if (logo != null) return new JLabel(logo);
        JLabel texto = new JLabel("<html><span color='#22C55E'>Venda</span>"
                + "<span color='#3B82F6'>Fácil</span></html>");
        texto.setFont(fonte(Font.BOLD, altura * 3 / 5));
        return texto;
    }

    // ------------------------------------------------------------------
    // Botões
    // ------------------------------------------------------------------

    public static class BotaoModerno extends JButton {
        private final Color base, hover, pressionado, borda;

        public BotaoModerno(String rotulo, Icon icone, Color base, Color hover,
                            Color pressionado, Color corTexto, Color borda) {
            super(rotulo, icone);
            this.base = base;
            this.hover = hover;
            this.pressionado = pressionado;
            this.borda = borda;
            setForeground(corTexto);
            setFont(fonte(Font.BOLD, 14));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setIconTextGap(8);
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color cor = base;
            if (getModel().isPressed()) cor = pressionado;
            else if (getModel().isRollover()) cor = hover;
            if (!isEnabled()) cor = misturar(base, Color.WHITE, 0.55f);
            g2.setColor(cor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARCO, ARCO);
            if (borda != null) {
                g2.setColor(borda);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARCO, ARCO);
            }
            if (isFocusOwner()) {
                g2.setColor(new Color(TEXTO.getRed(), TEXTO.getGreen(), TEXTO.getBlue(), 70));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARCO, ARCO);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton botaoPrimario(String rotulo, Icon icone) {
        return new BotaoModerno(rotulo, icone, VERDE, VERDE_HOVER, VERDE_PRESSION,
                Color.WHITE, null);
    }

    public static JButton botaoAzul(String rotulo, Icon icone) {
        return new BotaoModerno(rotulo, icone, AZUL, AZUL_HOVER,
                AZUL_HOVER.darker(), Color.WHITE, null);
    }

    public static JButton botaoPerigo(String rotulo, Icon icone) {
        return new BotaoModerno(rotulo, icone, PERIGO, PERIGO_HOVER,
                PERIGO_HOVER.darker(), Color.WHITE, null);
    }

    public static JButton botaoSecundario(String rotulo, Icon icone) {
        return new BotaoModerno(rotulo, icone, CARTAO, FUNDO, BORDA, TEXTO, BORDA);
    }

    private static Color misturar(Color a, Color b, float fator) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * fator),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * fator),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * fator));
    }

    // ------------------------------------------------------------------
    // Campos de texto
    // ------------------------------------------------------------------

    /** Pinta fundo arredondado, contorno (verde quando focado) e placeholder. */
    private static void pintarCampo(Graphics g, JTextComponentInfo info) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(info.habilitado ? CARTAO : FUNDO);
        g2.fillRoundRect(0, 0, info.largura, info.altura, ARCO, ARCO);
        g2.setColor(info.focado ? VERDE : BORDA);
        g2.setStroke(new BasicStroke(info.focado ? 2f : 1.2f));
        g2.drawRoundRect(1, 1, info.largura - 3, info.altura - 3, ARCO, ARCO);
        g2.dispose();
    }

    private static void pintarPlaceholder(Graphics g, JComponent campo,
                                          String placeholder) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(TEXTO_SUAVE);
        g2.setFont(campo.getFont());
        FontMetrics fm = g2.getFontMetrics();
        Insets in = campo.getInsets();
        int y = (campo.getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(placeholder, in.left, y);
        g2.dispose();
    }

    private record JTextComponentInfo(int largura, int altura,
                                      boolean focado, boolean habilitado) {}

    public static class CampoTexto extends JTextField {
        private final String placeholder;

        public CampoTexto(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(fonte(Font.PLAIN, 14));
            setForeground(TEXTO);
            setCaretColor(TEXTO);
            setBorder(new EmptyBorder(10, 14, 10, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            pintarCampo(g, new JTextComponentInfo(getWidth(), getHeight(),
                    isFocusOwner(), isEnabled()));
            super.paintComponent(g);
            if (placeholder != null && getText().isEmpty())
                pintarPlaceholder(g, this, placeholder);
        }
    }

    public static class CampoSenha extends JPasswordField {
        private final String placeholder;

        public CampoSenha(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setFont(fonte(Font.PLAIN, 14));
            setForeground(TEXTO);
            setCaretColor(TEXTO);
            setBorder(new EmptyBorder(10, 14, 10, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            pintarCampo(g, new JTextComponentInfo(getWidth(), getHeight(),
                    isFocusOwner(), isEnabled()));
            super.paintComponent(g);
            if (placeholder != null && getPassword().length == 0)
                pintarPlaceholder(g, this, placeholder);
        }
    }

    // ------------------------------------------------------------------
    // Cartão (painel branco arredondado com sombra suave)
    // ------------------------------------------------------------------

    public static class Cartao extends JPanel {
        private static final int SOMBRA = 5;

        public Cartao() {
            setOpaque(false);
            setBorder(new EmptyBorder(SOMBRA + 18, SOMBRA + 18,
                    SOMBRA + 18, SOMBRA + 18));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < SOMBRA; i++) {
                g2.setColor(new Color(15, 23, 42, 3 + i * 3));
                g2.fillRoundRect(i, i + 1, getWidth() - i * 2,
                        getHeight() - i * 2 - 1, ARCO + SOMBRA - i, ARCO + SOMBRA - i);
            }
            g2.setColor(CARTAO);
            g2.fillRoundRect(SOMBRA, SOMBRA, getWidth() - SOMBRA * 2,
                    getHeight() - SOMBRA * 2, ARCO, ARCO);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Etiqueta tipo "pill" (fundo arredondado colorido). */
    public static class EtiquetaPill extends JLabel {
        private Color fundo;

        public EtiquetaPill() {
            setOpaque(false);
            setFont(fonte(Font.BOLD, 12));
            setHorizontalAlignment(CENTER);
            setBorder(new EmptyBorder(4, 12, 4, 12));
        }

        public void configurar(String texto, Color fundo, Color corTexto) {
            setText(texto);
            this.fundo = fundo;
            setForeground(corTexto);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (fundo != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fundo);
                int alt = getPreferredSize().height;
                int y = (getHeight() - alt) / 2;
                g2.fillRoundRect(0, y, getWidth(), alt, alt, alt);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // ------------------------------------------------------------------
    // Tabelas e rolagem
    // ------------------------------------------------------------------

    /** Renderer padrão com zebra e espaçamento interno. */
    public static class RendererLinha extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor,
                boolean selecionada, boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, selecionada,
                    false, linha, coluna);
            setBorder(new EmptyBorder(0, 14, 0, 14));
            if (!selecionada) {
                setBackground(linha % 2 == 0 ? CARTAO : LINHA_ZEBRA);
                setForeground(TEXTO);
            }
            return this;
        }
    }

    public static void estilizarTabela(JTable tabela) {
        tabela.setRowHeight(42);
        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setFillsViewportHeight(true);
        tabela.setBackground(CARTAO);
        tabela.setFont(fonte(Font.PLAIN, 14));
        tabela.setForeground(TEXTO);
        tabela.setSelectionBackground(VERDE_CLARO);
        tabela.setSelectionForeground(TEXTO);
        tabela.setDefaultRenderer(Object.class, new RendererLinha());

        JTableHeader cabecalho = tabela.getTableHeader();
        cabecalho.setReorderingAllowed(false);
        cabecalho.setResizingAllowed(true);
        cabecalho.setPreferredSize(new Dimension(0, 44));
        cabecalho.setDefaultRenderer((tab, valor, sel, foco, lin, col) -> {
            JLabel r = new JLabel(String.valueOf(valor));
            r.setOpaque(true);
            r.setBackground(CARTAO);
            r.setForeground(TEXTO_SUAVE);
            r.setFont(fonte(Font.BOLD, 12));
            r.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, BORDA),
                    new EmptyBorder(0, 14, 0, 14)));
            return r;
        });
    }

    /** Deixa o campo do JSpinner alinhado à esquerda e com respiro interno. */
    public static void estilizarSpinner(JSpinner spinner) {
        spinner.setFont(fonte(Font.PLAIN, 14));
        JFormattedTextField campo =
                ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
        campo.setHorizontalAlignment(JTextField.LEFT);
        campo.setBorder(new EmptyBorder(0, 12, 0, 12));
        campo.setBackground(CARTAO);
        campo.setForeground(TEXTO);
        campo.setColumns(6);
    }

    public static JScrollPane rolagemSuave(Component conteudo) {
        JScrollPane rolagem = new JScrollPane(conteudo);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getViewport().setBackground(CARTAO);
        rolagem.setOpaque(false);
        for (JScrollBar barra : new JScrollBar[]{
                rolagem.getVerticalScrollBar(), rolagem.getHorizontalScrollBar()}) {
            barra.setUI(new BarraRolagemModerna());
            barra.setPreferredSize(new Dimension(10, 10));
            barra.setOpaque(false);
        }
        rolagem.getVerticalScrollBar().setUnitIncrement(16);
        return rolagem;
    }

    public static class BarraRolagemModerna extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            thumbColor = new Color(0xCBD5E1);
        }

        @Override
        protected JButton createDecreaseButton(int orientacao) { return botaoNulo(); }

        @Override
        protected JButton createIncreaseButton(int orientacao) { return botaoNulo(); }

        private JButton botaoNulo() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isDragging ? new Color(0x94A3B8) : thumbColor);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Ícones vetoriais (grade 24x24, estilo traço)
    // ------------------------------------------------------------------

    public static Icon icone(String tipo, int tamanho, Color cor) {
        return new IconeVetor(tipo, tamanho, cor);
    }

    private record IconeVetor(String tipo, int tamanho, Color cor) implements Icon {
        @Override public int getIconWidth() { return tamanho; }
        @Override public int getIconHeight() { return tamanho; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            double escala = tamanho / 24.0;
            g2.scale(escala, escala);
            g2.setColor(cor);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            desenhar(g2);
            g2.dispose();
        }

        private void desenhar(Graphics2D g2) {
            switch (tipo) {
                case "dashboard" -> {
                    g2.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 7, 3, 3));
                    g2.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 7, 3, 3));
                    g2.draw(new RoundRectangle2D.Double(3.5, 13.5, 7, 7, 3, 3));
                    g2.draw(new RoundRectangle2D.Double(13.5, 13.5, 7, 7, 3, 3));
                }
                case "caixa" -> {
                    g2.draw(new RoundRectangle2D.Double(3.5, 4.5, 17, 5, 2, 2));
                    Path2D corpo = new Path2D.Double();
                    corpo.moveTo(5.5, 9.5); corpo.lineTo(5.5, 19.5);
                    corpo.lineTo(18.5, 19.5); corpo.lineTo(18.5, 9.5);
                    g2.draw(corpo);
                    g2.draw(new Line2D.Double(10, 13, 14, 13));
                }
                case "carrinho" -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(3, 4.5); p.lineTo(6, 4.5); p.lineTo(8.6, 14.5);
                    p.lineTo(18.4, 14.5); p.lineTo(20.7, 7.5); p.lineTo(6.8, 7.5);
                    g2.draw(p);
                    g2.fill(new Ellipse2D.Double(8.6, 17.2, 3.6, 3.6));
                    g2.fill(new Ellipse2D.Double(15.4, 17.2, 3.6, 3.6));
                }
                case "sair" -> {
                    Path2D porta = new Path2D.Double();
                    porta.moveTo(13.5, 4.5); porta.lineTo(5.5, 4.5);
                    porta.lineTo(5.5, 19.5); porta.lineTo(13.5, 19.5);
                    g2.draw(porta);
                    g2.draw(new Line2D.Double(10.5, 12, 20.5, 12));
                    g2.draw(new Line2D.Double(17.2, 8.7, 20.5, 12));
                    g2.draw(new Line2D.Double(17.2, 15.3, 20.5, 12));
                }
                case "mais" -> {
                    g2.draw(new Line2D.Double(12, 5.5, 12, 18.5));
                    g2.draw(new Line2D.Double(5.5, 12, 18.5, 12));
                }
                case "lapis" -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(4.5, 19.5); p.lineTo(5.3, 16); p.lineTo(15.4, 5.9);
                    p.lineTo(18.5, 9); p.lineTo(8.4, 19.1); p.closePath();
                    g2.draw(p);
                    g2.draw(new Line2D.Double(13.4, 7.9, 16.5, 11));
                }
                case "lixeira" -> {
                    g2.draw(new Line2D.Double(4.5, 7, 19.5, 7));
                    Path2D alca = new Path2D.Double();
                    alca.moveTo(9.3, 7); alca.lineTo(9.7, 4.5);
                    alca.lineTo(14.3, 4.5); alca.lineTo(14.7, 7);
                    g2.draw(alca);
                    Path2D corpo = new Path2D.Double();
                    corpo.moveTo(6.3, 7.2); corpo.lineTo(7.3, 19.5);
                    corpo.lineTo(16.7, 19.5); corpo.lineTo(17.7, 7.2);
                    g2.draw(corpo);
                    g2.draw(new Line2D.Double(10.2, 10.3, 10.4, 16.3));
                    g2.draw(new Line2D.Double(13.8, 10.3, 13.6, 16.3));
                }
                case "lupa" -> {
                    g2.draw(new Ellipse2D.Double(4, 4, 11.5, 11.5));
                    g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(14.5, 14.5, 20, 20));
                }
                case "alerta" -> {
                    Path2D t = new Path2D.Double();
                    t.moveTo(12, 4); t.lineTo(21, 19.5); t.lineTo(3, 19.5);
                    t.closePath();
                    g2.draw(t);
                    g2.draw(new Line2D.Double(12, 9.5, 12, 13.8));
                    g2.fill(new Ellipse2D.Double(11, 15.6, 2, 2));
                }
                case "grafico" -> {
                    g2.draw(new Line2D.Double(4, 20, 20.5, 20));
                    g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(7.2, 16.6, 7.2, 12));
                    g2.draw(new Line2D.Double(12.2, 16.6, 12.2, 6.5));
                    g2.draw(new Line2D.Double(17.2, 16.6, 17.2, 9.5));
                }
                case "dinheiro" -> {
                    g2.draw(new RoundRectangle2D.Double(3, 6.5, 18, 11.5, 3, 3));
                    g2.draw(new Ellipse2D.Double(9.7, 9.5, 4.6, 5.4));
                    g2.fill(new Ellipse2D.Double(5.2, 11.3, 1.6, 1.6));
                    g2.fill(new Ellipse2D.Double(17.2, 11.3, 1.6, 1.6));
                }
                case "usuario" -> {
                    g2.draw(new Ellipse2D.Double(8.4, 4, 7.2, 7.2));
                    g2.draw(new Arc2D.Double(5, 13.4, 14, 12.4, 0, 180, Arc2D.OPEN));
                }
                case "cadeado" -> {
                    g2.draw(new RoundRectangle2D.Double(5.5, 11, 13, 8.8, 2.5, 2.5));
                    g2.draw(new Arc2D.Double(8.2, 4.2, 7.6, 9.5, 0, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Double(12, 14, 12, 16.4));
                }
                case "check" -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(5, 12.5); p.lineTo(10, 17.5); p.lineTo(19, 7);
                    g2.draw(p);
                }
                default -> {}
            }
        }
    }
}

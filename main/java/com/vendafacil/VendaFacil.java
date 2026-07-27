package com.vendafacil;

import com.vendafacil.app.Contexto;
import com.vendafacil.config.Configuracao;
import com.vendafacil.ui.Tema;
import com.vendafacil.ui.TelaLogin;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Ponto de entrada do VendaFácil.
 *
 * <p>Abre o banco antes de mostrar qualquer janela — se os dados não puderem
 * ser carregados, é melhor avisar de cara do que travar depois do login. Toda
 * a interface é criada na Event Dispatch Thread, como o Swing exige.
 */
public final class VendaFacil {

    private VendaFacil() {}

    public static void main(String[] args) {
        Contexto contexto;
        try {
            contexto = Contexto.iniciar(Configuracao.padrao());
        } catch (RuntimeException e) {
            mostrarFalhaFatal(e);
            System.exit(1);
            return;
        }

        // Fecha a conexão de forma ordenada, inclusive quando a janela é
        // fechada com EXIT_ON_CLOSE.
        Runtime.getRuntime().addShutdownHook(new Thread(contexto::close, "fechar-banco"));

        SwingUtilities.invokeLater(() -> {
            Tema.aplicarAjustesGlobais();
            new TelaLogin(contexto).setVisible(true);
        });
    }

    private static void mostrarFalhaFatal(RuntimeException e) {
        String mensagem = "Não foi possível iniciar o VendaFácil.\n\n"
                + (e.getMessage() == null ? e.toString() : e.getMessage());
        System.err.println(mensagem);
        e.printStackTrace();
        if (!java.awt.GraphicsEnvironment.isHeadless()) {
            JOptionPane.showMessageDialog(null, mensagem, "VendaFácil",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

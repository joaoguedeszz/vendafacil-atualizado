package com.vendafacil.servico;

import com.vendafacil.dominio.Credencial;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.suporte.BancoTeste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutenticacaoServicoTest {

    private BancoTeste banco;
    private AutenticacaoServico autenticacao;

    @BeforeEach
    void abrir() {
        banco = new BancoTeste();
        autenticacao = new AutenticacaoServico(banco.usuarios());
    }

    @AfterEach
    void fechar() {
        banco.close();
    }

    private static char[] senha(String texto) {
        return texto.toCharArray();
    }

    @Test
    @DisplayName("cria o usuário padrão apenas na primeira vez")
    void criaUsuarioPadraoUmaVez() {
        assertTrue(autenticacao.garantirUsuarioPadrao());
        assertFalse(autenticacao.garantirUsuarioPadrao());
        assertEquals(1, banco.usuarios().contar());
    }

    @Test
    @DisplayName("admin/1234 continua sendo o acesso padrão")
    void aceitaCredencialPadrao() {
        autenticacao.garantirUsuarioPadrao();

        assertEquals("admin", autenticacao.autenticar("admin", senha("1234")).orElseThrow());
    }

    @Test
    @DisplayName("recusa senha errada e usuário inexistente")
    void recusaCredenciaisInvalidas() {
        autenticacao.garantirUsuarioPadrao();

        assertTrue(autenticacao.autenticar("admin", senha("errada")).isEmpty());
        assertTrue(autenticacao.autenticar("admin", senha("")).isEmpty());
        assertTrue(autenticacao.autenticar("ninguem", senha("1234")).isEmpty());
    }

    @Test
    @DisplayName("login não diferencia maiúsculas nem espaços em volta")
    void normalizaLogin() {
        autenticacao.garantirUsuarioPadrao();

        assertTrue(autenticacao.autenticar("ADMIN", senha("1234")).isPresent());
        assertTrue(autenticacao.autenticar("  Admin  ", senha("1234")).isPresent());
    }

    @Test
    @DisplayName("a senha nunca é guardada em texto puro")
    void naoGuardaSenhaEmTextoPuro() {
        autenticacao.garantirUsuarioPadrao();

        Credencial guardada = banco.usuarios().porLogin("admin").orElseThrow();
        assertNotEquals("1234", guardada.hashSenha());
        assertFalse(guardada.hashSenha().contains("1234"));
        assertTrue(guardada.iteracoes() > 0);
        assertFalse(guardada.salt().isBlank());
    }

    @Test
    @DisplayName("cada usuário recebe um salt diferente")
    void saltEhUnicoPorUsuario() {
        Credencial a = autenticacao.cadastrar("ana", senha("mesmasenha"));
        Credencial b = autenticacao.cadastrar("bruno", senha("mesmasenha"));

        assertNotEquals(a.salt(), b.salt());
        assertNotEquals(a.hashSenha(), b.hashSenha(),
                "senhas iguais com salts diferentes devem gerar hashes diferentes");
    }

    @Test
    @DisplayName("o array da senha é zerado depois do uso")
    void limpaSenhaDaMemoria() {
        autenticacao.garantirUsuarioPadrao();
        char[] tentativa = senha("1234");

        autenticacao.autenticar("admin", tentativa);

        assertArrayZerado(tentativa);
    }

    @Test
    @DisplayName("recusa cadastro duplicado ou incompleto")
    void recusaCadastroInvalido() {
        autenticacao.cadastrar("ana", senha("segredo"));

        assertThrows(RegraDeNegocioException.class,
                () -> autenticacao.cadastrar("ANA", senha("outra")));
        assertThrows(RegraDeNegocioException.class,
                () -> autenticacao.cadastrar("  ", senha("segredo")));
        assertThrows(RegraDeNegocioException.class,
                () -> autenticacao.cadastrar("bruno", new char[0]));
    }

    private static void assertArrayZerado(char[] array) {
        for (char c : array) {
            assertEquals('\0', c, "a senha deveria ter sido apagada da memória");
        }
    }
}

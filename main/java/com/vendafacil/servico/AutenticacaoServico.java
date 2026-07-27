package com.vendafacil.servico;

import com.vendafacil.dominio.Credencial;
import com.vendafacil.dominio.RegraDeNegocioException;
import com.vendafacil.repositorio.UsuarioRepositorio;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

/**
 * Login do sistema.
 *
 * <p>Antes, usuário e senha eram duas constantes dentro da tela de login e a
 * comparação era feita em texto puro. Agora a credencial mora no banco com a
 * senha derivada por PBKDF2 — o acesso padrão continua sendo
 * {@code admin} / {@code 1234}, criado na primeira execução.
 */
public class AutenticacaoServico {

    public static final String LOGIN_PADRAO = "admin";
    public static final char[] SENHA_PADRAO = {'1', '2', '3', '4'};

    private final UsuarioRepositorio usuarios;

    public AutenticacaoServico(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    /**
     * Cria o usuário padrão se ainda não existir nenhum.
     *
     * @return true se o usuário padrão foi criado agora.
     */
    public boolean garantirUsuarioPadrao() {
        if (usuarios.contar() > 0) return false;
        cadastrar(LOGIN_PADRAO, SENHA_PADRAO.clone());
        return true;
    }

    /**
     * Confere as credenciais.
     *
     * <p>O array da senha é zerado antes de sair do método: {@code String} de
     * senha ficaria à deriva na memória até o coletor passar.
     *
     * @return o login autenticado, ou vazio se usuário ou senha estiverem errados.
     */
    public Optional<String> autenticar(String login, char[] senha) {
        try {
            Optional<Credencial> encontrada = usuarios.porLogin(login);
            if (encontrada.isEmpty()) return Optional.empty();
            Credencial c = encontrada.get();
            String tentativa = Senhas.derivar(senha, c.salt(), c.iteracoes());
            return Senhas.conferem(tentativa, c.hashSenha())
                    ? Optional.of(c.login())
                    : Optional.empty();
        } finally {
            Arrays.fill(senha, '\0');
        }
    }

    /** Cadastra uma credencial nova. O array da senha é zerado no fim. */
    public Credencial cadastrar(String login, char[] senha) {
        try {
            if (login == null || login.isBlank())
                throw new RegraDeNegocioException("Informe o usuário.");
            if (senha == null || senha.length == 0)
                throw new RegraDeNegocioException("Informe a senha.");
            String normalizado = Credencial.normalizarLogin(login);
            if (usuarios.porLogin(normalizado).isPresent())
                throw new RegraDeNegocioException(
                        "Já existe um usuário \"" + normalizado + "\".");

            String salt = Senhas.novoSalt();
            String hash = Senhas.derivar(senha, salt, Senhas.ITERACOES);
            return usuarios.inserir(new Credencial(Credencial.SEM_ID, normalizado,
                    hash, salt, Senhas.ITERACOES,
                    LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
        } finally {
            if (senha != null) Arrays.fill(senha, '\0');
        }
    }
}

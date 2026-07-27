package com.vendafacil.dominio;

import java.time.LocalDateTime;

/**
 * Credencial de acesso de um usuário.
 *
 * <p>A senha nunca é guardada: o que fica no banco é o resultado do PBKDF2
 * sobre ela, junto com o salt e o número de iterações usados — assim é
 * possível aumentar o custo do hash no futuro sem invalidar as senhas antigas.
 *
 * @param hashSenha PBKDF2-HMAC-SHA256 em Base64.
 * @param salt      salt aleatório em Base64.
 */
public record Credencial(long id, String login, String hashSenha, String salt,
                         int iteracoes, LocalDateTime criadoEm) {

    public static final long SEM_ID = 0L;

    public Credencial {
        if (login == null || login.isBlank())
            throw new RegraDeNegocioException("Informe o usuário.");
        login = normalizarLogin(login);
    }

    public static String normalizarLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase(java.util.Locale.ROOT);
    }
}

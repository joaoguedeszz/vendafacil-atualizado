package com.vendafacil.servico;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Derivação e verificação de senhas com PBKDF2-HMAC-SHA256.
 *
 * <p>Tudo vem do JDK — nenhuma biblioteca de criptografia é necessária.
 * A senha em si nunca é gravada nem registrada em log; o que se guarda é o
 * hash, o salt e o custo usados (veja {@link com.vendafacil.dominio.Credencial}).
 */
final class Senhas {

    /**
     * Custo do hash. Vale ~100 ms em um desktop atual: alto o bastante para
     * atrapalhar força bruta, baixo o bastante para o login não travar.
     * Aumentar este valor não invalida senhas antigas — cada credencial guarda
     * o número de iterações com que foi criada.
     */
    static final int ITERACOES = 120_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int BYTES_SALT = 16;
    private static final int BITS_CHAVE = 256;

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senhas() {}

    static String novoSalt() {
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    static String derivar(char[] senha, String saltBase64, int iteracoes) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, BITS_CHAVE);
        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            return Base64.getEncoder().encodeToString(fabrica.generateSecret(spec).getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Algoritmo de senha indisponível na JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Comparação em tempo constante — evita vazar o hash por tempo de resposta. */
    static boolean conferem(String hashA, String hashB) {
        if (hashA == null || hashB == null) return false;
        return MessageDigest.isEqual(
                hashA.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                hashB.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

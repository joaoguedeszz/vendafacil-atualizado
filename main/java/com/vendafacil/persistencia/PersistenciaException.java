package com.vendafacil.persistencia;

/**
 * Falha técnica ao falar com o banco de dados (I/O, SQL inválido, arquivo
 * corrompido).
 *
 * <p>Diferente de {@link com.vendafacil.dominio.RegraDeNegocioException}, que
 * significa "o usuário digitou algo inválido", esta exceção significa "o
 * sistema está com problema" — a interface a trata como erro, não como
 * validação de formulário.
 */
public class PersistenciaException extends RuntimeException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }
}

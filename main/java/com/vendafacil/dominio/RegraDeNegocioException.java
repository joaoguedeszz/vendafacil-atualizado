package com.vendafacil.dominio;

/**
 * Violação de uma regra de negócio ou de um dado informado pelo usuário.
 *
 * <p>A mensagem é escrita para ser exibida diretamente na interface, então
 * deve ser sempre em português e sem detalhes técnicos.
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}

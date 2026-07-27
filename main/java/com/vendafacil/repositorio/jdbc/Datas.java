package com.vendafacil.repositorio.jdbc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Conversão entre {@link LocalDateTime} e o texto gravado no banco.
 *
 * <p>Formato ISO-8601 com segundos sempre presentes: assim a ordenação
 * alfabética da coluna coincide com a ordem cronológica, o que deixa os
 * índices por data funcionarem.
 */
final class Datas {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Datas() {}

    static String paraTexto(LocalDateTime data) {
        return FORMATO.format(data);
    }

    static LocalDateTime deTexto(String texto) {
        return LocalDateTime.parse(texto, FORMATO);
    }
}

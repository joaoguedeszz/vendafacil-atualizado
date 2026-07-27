package com.vendafacil.repositorio;

import com.vendafacil.dominio.Credencial;

import java.util.Optional;

/** Acesso às credenciais de acesso ao sistema. */
public interface UsuarioRepositorio {

    Credencial inserir(Credencial nova);

    Optional<Credencial> porLogin(String login);

    int contar();
}

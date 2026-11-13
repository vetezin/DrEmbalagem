package br.com.doutorembalagem.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo de POST /api/auth/login. */
public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank String senha) {
    @Override public String toString() { return "LoginRequest[protegido]"; }
}

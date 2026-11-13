package br.com.doutorembalagem.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /api/auth/trocar-senha. Confirmacao nao e persistida. */
public record TrocarSenhaRequest(
        @NotBlank String novaSenha,
        @NotBlank String confirmacao) {
    @Override public String toString() { return "TrocarSenhaRequest[protegido]"; }
}

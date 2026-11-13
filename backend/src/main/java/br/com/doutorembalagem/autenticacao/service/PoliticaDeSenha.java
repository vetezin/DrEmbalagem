package br.com.doutorembalagem.autenticacao.service;

import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import java.nio.charset.StandardCharsets;

/** Regra de senha: >= 8 caracteres, >= 1 numero, >= 1 especial (nao branco), <= 72 bytes. */
public final class PoliticaDeSenha {

    private PoliticaDeSenha() {
    }

    public static void validar(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new RequisicaoInvalidaException("Senha obrigatoria.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RequisicaoInvalidaException("Senha excede o limite de 72 bytes.");
        }
        if (senha.codePointCount(0, senha.length()) < 8) {
            throw new RequisicaoInvalidaException("Senha deve ter no minimo 8 caracteres.");
        }
        if (senha.codePoints().noneMatch(Character::isDigit)) {
            throw new RequisicaoInvalidaException("Senha deve conter pelo menos um numero.");
        }
        boolean temEspecial = senha.codePoints().anyMatch(c ->
                !Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && !Character.isSpaceChar(c));
        if (!temEspecial) {
            throw new RequisicaoInvalidaException("Senha deve conter pelo menos um caractere especial.");
        }
    }
}

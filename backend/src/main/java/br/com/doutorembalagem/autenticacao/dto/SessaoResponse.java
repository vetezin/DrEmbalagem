package br.com.doutorembalagem.autenticacao.dto;

import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import java.util.List;

/** Identidade autenticada; nunca inclui hash de senha. */
public record SessaoResponse(
        Long id,
        String nome,
        String email,
        NivelUsuario nivel,
        Long empresaId,
        List<String> permissoes,
        boolean trocaSenhaObrigatoria) {
}

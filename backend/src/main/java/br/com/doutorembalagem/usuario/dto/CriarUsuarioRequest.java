package br.com.doutorembalagem.usuario.dto;

import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import jakarta.validation.constraints.*;
import java.util.Set;

public record CriarUsuarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 254) String email,
        @NotBlank String senhaTemporaria,
        @NotNull NivelUsuario nivel,
        @NotNull @Positive Long empresaId,
        @NotNull Set<@NotNull Permissao> permissoes) {
    @Override public String toString() { return "CriarUsuarioRequest[protegido]"; }
}

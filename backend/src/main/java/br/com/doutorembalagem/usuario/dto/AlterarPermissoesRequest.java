package br.com.doutorembalagem.usuario.dto;

import br.com.doutorembalagem.usuario.enums.Permissao;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AlterarPermissoesRequest(@NotNull Set<@NotNull Permissao> permissoes) { }

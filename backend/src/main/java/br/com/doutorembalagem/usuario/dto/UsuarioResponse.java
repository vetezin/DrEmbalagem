package br.com.doutorembalagem.usuario.dto;

import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.Usuario;
import java.util.List;
import java.util.Set;

public record UsuarioResponse(Long id, String nome, String email, NivelUsuario nivel, Long empresaId,
        List<String> permissoes, boolean trocaSenhaObrigatoria, boolean ativo) {
    public static UsuarioResponse de(Usuario u, Set<Permissao> permissoes) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getNivel(),
                u.getEmpresa() == null ? null : u.getEmpresa().getId(),
                permissoes.stream().map(Enum::name).sorted().toList(), u.isTrocaSenhaObrigatoria(), u.isAtivo());
    }
}

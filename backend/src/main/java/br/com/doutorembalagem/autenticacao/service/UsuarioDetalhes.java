package br.com.doutorembalagem.autenticacao.service;

import br.com.doutorembalagem.autenticacao.dto.SessaoResponse;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Identidade autenticada carregada na sessao. Nao guarda a entidade JPA;
 * carrega apenas os campos necessarios, evitando proxies LAZY fora da transacao.
 * Nunca e serializado para resposta (hash fica somente em memoria de sessao).
 */
public record UsuarioDetalhes(
        Long id,
        String nome,
        String email,
        String senhaHash,
        NivelUsuario nivel,
        Long empresaId,
        boolean trocaSenhaObrigatoria,
        boolean ativo,
        Set<Permissao> permissoes) implements UserDetails {

    @Override
    public String toString() {
        return "UsuarioDetalhes[id=" + id + ", nivel=" + nivel + "]";
    }

    public static UsuarioDetalhes de(Usuario usuario, Set<Permissao> permissoes) {
        return new UsuarioDetalhes(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getSenhaHash(),
                usuario.getNivel(),
                usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId(),
                usuario.isTrocaSenhaObrigatoria(),
                usuario.isAtivo(),
                Set.copyOf(permissoes));
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        for (Permissao permissao : permissoes) {
            autoridades.add(new SimpleGrantedAuthority("PERM_" + permissao.name()));
        }
        if (nivel == NivelUsuario.SUPERADMIN) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
        }
        if (trocaSenhaObrigatoria) {
            autoridades.add(new SimpleGrantedAuthority("TROCA_SENHA_PENDENTE"));
        }
        return autoridades;
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }

    public SessaoResponse paraSessao() {
        return new SessaoResponse(id, nome, email, nivel, empresaId,
                permissoes.stream().map(Enum::name).sorted().toList(), trocaSenhaObrigatoria);
    }
}

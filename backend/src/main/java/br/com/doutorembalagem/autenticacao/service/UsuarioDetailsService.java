package br.com.doutorembalagem.autenticacao.service;

import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import br.com.doutorembalagem.usuario.model.UsuarioPermissaoId;
import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/** Carrega a identidade por email case-insensitive, com permissoes do catalogo. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.buscarPorEmailComEmpresa(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado."));
        Set<br.com.doutorembalagem.usuario.enums.Permissao> permissoes = usuarioRepository
                .findPermissoesByUsuarioId(usuario.getId()).stream()
                .map(UsuarioPermissao::getId)
                .map(UsuarioPermissaoId::getPermissao)
                .collect(Collectors.toSet());
        return UsuarioDetalhes.de(usuario, permissoes);
    }
}

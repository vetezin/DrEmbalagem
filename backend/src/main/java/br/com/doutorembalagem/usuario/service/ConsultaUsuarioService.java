package br.com.doutorembalagem.usuario.service;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.erro.SessaoInvalidaException;
import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import br.com.doutorembalagem.usuario.model.UsuarioPermissaoId;
import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/** Leitura da identidade atualizada do banco para a sessao. */
@Service
public class ConsultaUsuarioService {

    private final UsuarioRepository usuarioRepository;

    public ConsultaUsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioDetalhes buscarPorId(Long id) {
        Usuario usuario = usuarioRepository.buscarPorIdComEmpresa(id)
                .orElseThrow(SessaoInvalidaException::new);
        if (!usuario.isAtivo()) throw new SessaoInvalidaException();
        Set<br.com.doutorembalagem.usuario.enums.Permissao> permissoes = usuarioRepository
                .findPermissoesByUsuarioId(usuario.getId()).stream()
                .map(UsuarioPermissao::getId)
                .map(UsuarioPermissaoId::getPermissao)
                .collect(Collectors.toSet());
        return UsuarioDetalhes.de(usuario, permissoes);
    }
}

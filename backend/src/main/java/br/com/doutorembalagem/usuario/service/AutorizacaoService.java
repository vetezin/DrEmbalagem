package br.com.doutorembalagem.usuario.service;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutorizacaoService {
    private final ConsultaUsuarioService consulta;

    @Transactional(readOnly = true)
    public UsuarioDetalhes atual(Long id) {
        UsuarioDetalhes ator = consulta.buscarPorId(id);
        if (ator.trocaSenhaObrigatoria()) negar();
        return ator;
    }

    public void administrar(UsuarioDetalhes ator, Permissao permissao) {
        if (ator.nivel() == NivelUsuario.SUPERADMIN) return;
        if (ator.nivel() != NivelUsuario.ADMIN || !ator.permissoes().contains(permissao)) negar();
    }

    public void superadmin(UsuarioDetalhes ator) {
        if (ator.nivel() != NivelUsuario.SUPERADMIN) negar();
    }

    public void empresa(UsuarioDetalhes ator, Long empresaId) {
        if (ator.nivel() != NivelUsuario.SUPERADMIN && !Objects.equals(ator.empresaId(), empresaId)) negar();
    }

    public void conceder(UsuarioDetalhes ator, NivelUsuario nivel, Long empresaId, Set<Permissao> permissoes) {
        if (nivel == NivelUsuario.SUPERADMIN) negar();
        empresa(ator, empresaId);
        if (ator.nivel() == NivelUsuario.SUPERADMIN) return;
        if (ator.nivel() != NivelUsuario.ADMIN || nivel != NivelUsuario.CONSULTOR
                || !ator.permissoes().containsAll(permissoes)) negar();
    }

    private static void negar() {
        throw new AccessDeniedException("Acesso negado.");
    }
}

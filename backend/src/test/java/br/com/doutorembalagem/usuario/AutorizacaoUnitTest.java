package br.com.doutorembalagem.usuario;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.*;
import br.com.doutorembalagem.usuario.service.AutorizacaoService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;

class AutorizacaoUnitTest {
    private final AutorizacaoService service = new AutorizacaoService(null);
    private UsuarioDetalhes ator(NivelUsuario nivel, Set<Permissao> permissoes) {
        return new UsuarioDetalhes(1L, "Nome", "a@b.com", "hash", nivel,
                nivel == NivelUsuario.SUPERADMIN ? null : 10L, false, true, permissoes);
    }
    @Test void superadminTemBypassMasNaoCriaOutroSuperadmin() {
        UsuarioDetalhes ator = ator(NivelUsuario.SUPERADMIN, Set.of());
        assertThatCode(() -> service.administrar(ator, Permissao.USUARIOS_CRIAR)).doesNotThrowAnyException();
        assertThatCode(() -> service.conceder(ator, NivelUsuario.ADMIN, 20L, Set.of(Permissao.USUARIOS_CRIAR))).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.conceder(ator, NivelUsuario.SUPERADMIN, null, Set.of())).isInstanceOf(AccessDeniedException.class);
    }
    @Test void adminSomenteConsultorDaPropriaEmpresaESubconjunto() {
        UsuarioDetalhes ator = ator(NivelUsuario.ADMIN, Set.of(Permissao.USUARIOS_CRIAR));
        assertThatCode(() -> service.conceder(ator, NivelUsuario.CONSULTOR, 10L, Set.of(Permissao.USUARIOS_CRIAR))).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.conceder(ator, NivelUsuario.CONSULTOR, 20L, Set.of())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.conceder(ator, NivelUsuario.ADMIN, 10L, Set.of())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.conceder(ator, NivelUsuario.CONSULTOR, 10L, Set.of(Permissao.USUARIOS_LISTAR))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void consultorMesmoComPermissoesNaoAdministra() {
        UsuarioDetalhes ator = ator(NivelUsuario.CONSULTOR, Set.of(Permissao.values()));
        assertThatThrownBy(() -> service.administrar(ator, Permissao.USUARIOS_CRIAR)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.conceder(ator, NivelUsuario.CONSULTOR, 10L, Set.of())).isInstanceOf(AccessDeniedException.class);
    }
}

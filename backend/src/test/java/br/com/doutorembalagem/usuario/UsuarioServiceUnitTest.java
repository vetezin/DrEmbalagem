package br.com.doutorembalagem.usuario;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import br.com.doutorembalagem.usuario.dto.*;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.*;
import br.com.doutorembalagem.usuario.repository.*;
import br.com.doutorembalagem.usuario.service.*;
import br.com.doutorembalagem.erro.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioServiceUnitTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EmpresaRepository empresas = mock(EmpresaRepository.class);
    private final ConsultaUsuarioService consulta = mock(ConsultaUsuarioService.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AutorizacaoService autorizacao = new AutorizacaoService(consulta);
    private final UsuarioPermissaoRepository permissoes = mock(UsuarioPermissaoRepository.class);
    private final UsuarioService service = new UsuarioService(usuarios, permissoes, empresas, autorizacao, encoder);

    @BeforeEach void ator() {
        when(consulta.buscarPorId(1L)).thenReturn(new UsuarioDetalhes(1L, "Admin", "admin@exemplo.com", "hash",
                NivelUsuario.ADMIN, 10L, false, true, Set.of(Permissao.values())));
    }
    @Test void payloadDeOutraEmpresaOuPromocaoNaoPersiste() {
        for (CriarUsuarioRequest pedido : List.of(
                new CriarUsuarioRequest("Nome", "email@exemplo.com", "Temporaria1!", NivelUsuario.CONSULTOR, 20L, Set.of()),
                new CriarUsuarioRequest("Nome", "email@exemplo.com", "Temporaria1!", NivelUsuario.ADMIN, 10L, Set.of()),
                new CriarUsuarioRequest("Nome", "email@exemplo.com", "Temporaria1!", NivelUsuario.SUPERADMIN, 10L, Set.of()))) {
            assertThatThrownBy(() -> service.criar(1L, pedido)).isInstanceOf(AccessDeniedException.class);
        }
        verifyNoInteractions(usuarios, permissoes, empresas, encoder);
    }
    @Test void primeiroAcessoBloqueiaServiceMesmoSemFiltroHttp() {
        when(consulta.buscarPorId(1L)).thenReturn(new UsuarioDetalhes(1L, "Admin", "a@b.com", "hash",
                NivelUsuario.SUPERADMIN, null, true, true, Set.of()));
        assertThatThrownBy(() -> service.listar(1L, 0, 20)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(usuarios, permissoes);
    }
    @Test void criacaoNormalizaEmailNomeEGravaSomenteHashComTrocaObrigatoria() {
        Empresa empresa = mock(Empresa.class);
        when(empresa.getId()).thenReturn(10L);
        when(empresas.findById(10L)).thenReturn(Optional.of(empresa));
        when(encoder.encode("Temporaria1!")).thenReturn("hash-seguro");
        when(usuarios.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        UsuarioResponse resposta = service.criar(1L, new CriarUsuarioRequest(" Nome ", " EMAIL@EXEMPLO.COM ", "Temporaria1!",
                NivelUsuario.CONSULTOR, 10L, Set.of(Permissao.USUARIOS_LISTAR)));
        assertThat(resposta.nome()).isEqualTo("Nome");
        assertThat(resposta.email()).isEqualTo("email@exemplo.com");
        assertThat(resposta.trocaSenhaObrigatoria()).isTrue();
        assertThat(resposta.ativo()).isTrue();
        verify(usuarios).saveAndFlush(argThat(u -> u.getSenhaHash().equals("hash-seguro") && u.getEmpresa() == empresa));
        verify(permissoes).save(any(UsuarioPermissao.class));
    }
    @Test void cadastroRejeitaDominioIncompletoOuInvalidoAntesDaPersistencia() {
        for (String email : List.of("teste@exemplo", "teste@exemplo..org", "teste@-exemplo.org")) {
            assertThatThrownBy(() -> service.criar(1L, new CriarUsuarioRequest("Nome", email, "SomenteFixture1!",
                    NivelUsuario.CONSULTOR, 10L, Set.of())))
                    .isInstanceOf(RequisicaoInvalidaException.class).hasMessage("Email invalido.");
        }
        verifyNoInteractions(usuarios, permissoes, empresas, encoder);
    }

    @Test void alvoDeOutraEmpresaRetorna404SemAlterarPermissoes() {
        Empresa terceira = mock(Empresa.class);
        when(terceira.getId()).thenReturn(20L);
        when(usuarios.buscarPorIdComEmpresa(5L)).thenReturn(Optional.of(new Usuario(terceira, "Outro", "o@e.com", "hash", NivelUsuario.CONSULTOR, true, true)));
        assertThatThrownBy(() -> service.alterarPermissoes(1L, 5L, new AlterarPermissoesRequest(Set.of())))
                .isInstanceOf(NaoEncontradoException.class);
        verifyNoInteractions(permissoes);
    }
    @Test void paginaInvalidaNaoExecutaConsulta() {
        assertThatThrownBy(() -> service.listar(1L, -1, 20)).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> service.listar(1L, 0, 101)).isInstanceOf(RequisicaoInvalidaException.class);
        verifyNoInteractions(usuarios, permissoes);
    }
}

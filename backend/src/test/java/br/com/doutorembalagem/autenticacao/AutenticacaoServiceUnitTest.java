package br.com.doutorembalagem.autenticacao;

import br.com.doutorembalagem.autenticacao.dto.TrocarSenhaRequest;
import br.com.doutorembalagem.autenticacao.service.AutenticacaoService;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.model.*;
import br.com.doutorembalagem.usuario.repository.*;
import br.com.doutorembalagem.erro.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutenticacaoServiceUnitTest {
    private final AuthenticationManager manager = mock(AuthenticationManager.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final UsuarioPermissaoRepository permissoes = mock(UsuarioPermissaoRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AutenticacaoService service = new AutenticacaoService(manager, usuarios, encoder);

    @Test void loginFalhoOuInativoUsaMesmoErroGenerico() {
        for (AuthenticationException falha : List.of(new BadCredentialsException("interno"), new DisabledException("interno"),
                new org.springframework.security.core.userdetails.UsernameNotFoundException("interno"))) {
            doThrow(falha).when(manager).authenticate(any());
            assertThatThrownBy(() -> service.autenticar(" A@B.COM ", "Senha123!"))
                    .isInstanceOf(CredenciaisInvalidasException.class).hasMessage("Credenciais invalidas.");
        }
    }
    @Test void loginPreservaIdentidadeLegadaSemAplicarNovaRegraDeCadastro() {
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes detalhes = new br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes(1L, "Nome", "legado@interno", "hash",
                NivelUsuario.SUPERADMIN, null, false, true, Set.of());
        when(manager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(detalhes, null, List.of()));
        assertThat(service.autenticar(" legado@interno ", "SomenteFixture1!")).isSameAs(detalhes);
        verify(manager).authenticate(argThat(a -> a.getPrincipal().equals("legado@interno")));
    }

    @Test void loginNaoTruncaMultibyte() {
        assertThatThrownBy(() -> service.autenticar("a@b.com", "á".repeat(37)))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verifyNoInteractions(manager);
    }
    @Test void trocaValidaSubstituiHashEObrigatoriedadeSemPersistirConfirmacao() {
        Usuario usuario = new Usuario(null, "Nome", "a@b.com", "antigo", NivelUsuario.SUPERADMIN, true, true);
        when(usuarios.buscarPorIdComEmpresa(1L)).thenReturn(Optional.of(usuario));
        when(permissoes.findByIdUsuarioId(null)).thenReturn(List.of());
        when(encoder.encode("NovaSenha1!")).thenReturn("novo-hash");
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes atualizado = service.trocarSenha(1L, new TrocarSenhaRequest("NovaSenha1!", "NovaSenha1!"));
        assertThat(usuario.getSenhaHash()).isEqualTo("novo-hash");
        assertThat(atualizado.trocaSenhaObrigatoria()).isFalse();
    }
    @Test void trocaRejeitaConfirmacaoSenhaIgualContaInativaETrocaJaConcluida() {
        assertThatThrownBy(() -> service.trocarSenha(1L, new TrocarSenhaRequest("NovaSenha1!", "OutraSenha1!")))
                .isInstanceOf(RequisicaoInvalidaException.class);
        Usuario usuario = new Usuario(null, "Nome", "a@b.com", "antigo", NivelUsuario.SUPERADMIN, true, true);
        when(usuarios.buscarPorIdComEmpresa(1L)).thenReturn(Optional.of(usuario));
        when(encoder.matches("NovaSenha1!", "antigo")).thenReturn(true);
        assertThatThrownBy(() -> service.trocarSenha(1L, new TrocarSenhaRequest("NovaSenha1!", "NovaSenha1!")))
                .isInstanceOf(RequisicaoInvalidaException.class);
        usuario.setAtivo(false);
        assertThatThrownBy(() -> service.trocarSenha(1L, new TrocarSenhaRequest("NovaSenha1!", "NovaSenha1!")))
                .isInstanceOf(SessaoInvalidaException.class);
        usuario.setAtivo(true);
        usuario.setTrocaSenhaObrigatoria(false);
        assertThatThrownBy(() -> service.trocarSenha(1L, new TrocarSenhaRequest("NovaSenha1!", "NovaSenha1!")))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(encoder, never()).encode(any());
    }
}

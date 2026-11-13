package br.com.doutorembalagem.autenticacao.config;

import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import java.util.function.Function;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuperadminBootstrapUnitTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    @SuppressWarnings("unchecked")
    private final Function<String, String> ambiente = mock(Function.class);

    private SuperadminBootstrap bootstrap(String nome, String email) {
        when(ambiente.apply("APP_SUPERADMIN_NOME")).thenReturn(nome);
        when(ambiente.apply("APP_SUPERADMIN_EMAIL")).thenReturn(email);
        when(ambiente.apply("APP_SUPERADMIN_SENHA")).thenReturn("SomenteFixture1!");
        when(encoder.encode(anyString())).thenReturn("hash-ficticio");
        return new SuperadminBootstrap(usuarios, encoder, ambiente);
    }

    @Test void nomeExplicitoNormalizadoENomePadrao() {
        for (String nome : new String[]{" Nome de Teste ", "a".repeat(120)}) {
            bootstrap(nome, " teste@exemplo.org ").run(null);
            verify(usuarios).save(argThat(u -> u.getNome().equals(nome.trim())
                    && u.getEmail().equals("teste@exemplo.org") && u.getSenhaHash().equals("hash-ficticio")
                    && u.getNivel() == NivelUsuario.SUPERADMIN && u.getEmpresa() == null && u.isTrocaSenhaObrigatoria()));
            clearInvocations(usuarios);
        }
        bootstrap(null, "teste@exemplo.com.br").run(null);
        verify(usuarios).save(argThat(u -> u.getNome().equals("Superadmin")));
    }

    @Test void nomeExplicitoInvalidoNaoPersiste() {
        for (String nome : new String[]{"", "   ", "a".repeat(121)}) {
            assertThatThrownBy(() -> bootstrap(nome, "teste@exemplo.com").run(null))
                    .isInstanceOf(RequisicaoInvalidaException.class);
        }
        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    @Test void emailInvalidoNaoPersisteNemGeraHash() {
        for (String email : new String[]{"teste@exemplo", "teste@exemplo..org", "teste@-exemplo.org"}) {
            assertThatThrownBy(() -> bootstrap("Nome", email).run(null))
                    .isInstanceOf(RequisicaoInvalidaException.class);
        }
        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    @Test void superadminExistenteNaoEAlteradoMesmoComConfiguracaoInvalida() {
        when(usuarios.existsByNivel(NivelUsuario.SUPERADMIN)).thenReturn(true);
        bootstrap(" ", "invalido").run(null);
        verify(usuarios, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }
}

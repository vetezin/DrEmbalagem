package br.com.doutorembalagem.usuario;

import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import br.com.doutorembalagem.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailUnitTest {
    @Test void aceitaDominiosCompletosSemRestringirTld() {
        for (String email : new String[]{"pessoa@exemplo.com", "pessoa@exemplo.com.br", "pessoa@exemplo.org",
                "nome+visita@sub.exemplo.technology", "pessoa@exemplo.xn--p1ai"}) {
            assertThat(UsuarioService.emailCanonico(" " + email.toUpperCase(java.util.Locale.ROOT) + " ")).isEqualTo(email);
        }
    }

    @Test void rejeitaDominioSemPontoOuMalformado() {
        for (String email : new String[]{"pessoa", "pessoa.exemplo.com", "pessoa@exemplo", "pessoa@@exemplo.com",
                "pessoa@.com", "pessoa@exemplo.", "pessoa@exemplo..com", "pessoa@-exemplo.com",
                "pessoa@exemplo-.com", "pessoa@exem_plo.com", "pessoa@exemplo.123", "pessoa@exemplo.c",
                "pessoa@ex emplo.com", "pessoa@" + "a".repeat(64) + ".com"}) {
            assertThatThrownBy(() -> UsuarioService.emailCanonico(email)).as(email)
                    .isInstanceOf(RequisicaoInvalidaException.class).hasMessage("Email invalido.");
        }
        assertThatThrownBy(() -> UsuarioService.emailCanonico(null)).isInstanceOf(RequisicaoInvalidaException.class);
    }
}

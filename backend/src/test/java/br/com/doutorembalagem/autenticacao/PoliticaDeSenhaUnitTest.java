package br.com.doutorembalagem.autenticacao;

import br.com.doutorembalagem.autenticacao.service.PoliticaDeSenha;
import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PoliticaDeSenhaUnitTest {
    @Test void aceitaSenhaSemTruncamentoInclusiveMultibyte() {
        assertThatCode(() -> PoliticaDeSenha.validar("á".repeat(34) + "a1!a")).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaDeSenha.validar("Senha 1! segura")).doesNotThrowAnyException();
    }
    @Test void espacoNaoSeparavelNaoContaComoEspecial() {
        assertThatThrownBy(() -> PoliticaDeSenha.validar("abcdef1\u00A0"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Senha deve conter pelo menos um caractere especial.");
    }
    @Test void espacoNaoSeparavelEstreitoNaoContaComoEspecial() {
        assertThatThrownBy(() -> PoliticaDeSenha.validar("abcdef1\u202F"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessage("Senha deve conter pelo menos um caractere especial.");
    }
    @Test void permiteEspacosUnicodeQuandoExisteEspecialValido() {
        assertThatCode(() -> PoliticaDeSenha.validar("abcdef1\u00A0\u202F!"))
                .doesNotThrowAnyException();
    }
    @Test void rejeitaPoliticaInvalidaELimiteEmBytes() {
        for (String valor : new String[]{"abc1!", "abcdefgh!", "abcdefgh1", "abcdef1 ", "á".repeat(35) + "a1!", "😀😀😀1!"}) {
            assertThatThrownBy(() -> PoliticaDeSenha.validar(valor)).isInstanceOf(RequisicaoInvalidaException.class);
        }
    }
}

package br.com.doutorembalagem.empresa;

import br.com.doutorembalagem.empresa.service.EmpresaService;
import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CnpjUnitTest {
    @Test void aceitaExemploOficialAlfanumerico() {
        assertThat(EmpresaService.normalizarCnpj(" 12.abc.345/01de-35 ")).isEqualTo("12ABC34501DE35");
    }
    @Test void preservaCompatibilidadeNumerica() {
        assertThat(EmpresaService.normalizarCnpj("12.345.678/0001-95")).isEqualTo("12345678000195");
    }
    @Test void rejeitaDvFormatoCaracteresEstranhosERepeticao() {
        for (String valor : new String[]{"12ABC34501DE36", "12345678000196", "00000000000000",
                "11111111111111", "12ABC34501DEAA", "12ÄBC34501DE35", "12 ABC34501DE35", "12ABC34501DE35!", "", "123"}) {
            assertThatThrownBy(() -> EmpresaService.normalizarCnpj(valor)).isInstanceOf(RequisicaoInvalidaException.class);
        }
        assertThatThrownBy(() -> EmpresaService.normalizarCnpj(null)).isInstanceOf(RequisicaoInvalidaException.class);
    }
}

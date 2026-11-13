package br.com.doutorembalagem.empresa;

import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@org.junit.jupiter.api.Disabled("Integracao bloqueada ate configuracao de banco descartavel isolado; nunca usar perfil database de desenvolvimento.")
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("database")
class EmpresaSchemaTest {

    @Autowired
    private EmpresaRepository empresaRepository;

    private Empresa empresa(String razaoSocial, String cnpj) {
        return new Empresa(razaoSocial, null, cnpj);
    }

    @Test
    void salvaEmpresaSemEnderecoEPreencheIdentidadeETimestamps() {
        Empresa salva = empresaRepository.saveAndFlush(empresa("Embalagens Exemplo Ltda", "12345678000195"));

        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getCriadoEm()).isNotNull();
        assertThat(salva.getAtualizadoEm()).isNotNull();
    }

    @Test
    void rejeitaCnpjDuplicado() {
        empresaRepository.saveAndFlush(empresa("Primeira Ltda", "12345678000195"));

        assertThatThrownBy(() -> empresaRepository.saveAndFlush(empresa("Segunda Ltda", "12345678000195")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_empresa_cnpj");
    }

    @Test
    void aceitaCnpjAlfanumerico() {
        Empresa salva = empresaRepository.saveAndFlush(empresa("Alfanum Ltda", "12ABC345678X95"));

        assertThat(salva.getId()).isNotNull();
    }
}

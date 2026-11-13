package br.com.doutorembalagem.empresa;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.empresa.dto.CriarEmpresaRequest;
import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import br.com.doutorembalagem.empresa.service.EmpresaService;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.service.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmpresaServiceUnitTest {
    private final EmpresaRepository repository = mock(EmpresaRepository.class);
    private final ConsultaUsuarioService consulta = mock(ConsultaUsuarioService.class);
    private final EmpresaService service = new EmpresaService(repository, new AutorizacaoService(consulta));
    private void ator(NivelUsuario nivel) {
        when(consulta.buscarPorId(1L)).thenReturn(new UsuarioDetalhes(1L, "Nome", "a@b.com", "hash", nivel,
                nivel == NivelUsuario.SUPERADMIN ? null : 10L, false, true, Set.of()));
    }
    @Test void empresaSemEnderecoOuParcialNormalizaDadosSemInventarObrigatoriedade() {
        ator(NivelUsuario.SUPERADMIN);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        br.com.doutorembalagem.empresa.dto.EmpresaResponse sem = service.criar(1L, new CriarEmpresaRequest(" Razao ", " ", "12.ABC.345/01DE-35",
                null, null, null, null, null, null, null));
        assertThat(sem.razaoSocial()).isEqualTo("Razao");
        assertThat(sem.nomeFantasia()).isNull();
        assertThat(sem.cnpj()).isEqualTo("12ABC34501DE35");
        assertThat(sem.cep()).isNull();
        br.com.doutorembalagem.empresa.dto.EmpresaResponse parcial = service.criar(1L, new CriarEmpresaRequest("Razao", null, "12345678000195",
                "12345-678", null, "S/N", null, null, null, "sp"));
        assertThat(parcial.cep()).isEqualTo("12345678");
        assertThat(parcial.numero()).isEqualTo("S/N");
        assertThat(parcial.uf()).isEqualTo("SP");
        assertThat(parcial.logradouro()).isNull();
    }
    @Test void adminNaoCriaEListaSomentePropriaEmpresa() {
        ator(NivelUsuario.ADMIN);
        assertThatThrownBy(() -> service.criar(1L, null)).isInstanceOf(AccessDeniedException.class);
        when(repository.findById(eq(10L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<Empresa>(java.util.List.of()));
        service.listar(1L, 0, 20);
        verify(repository).findById(eq(10L), any(org.springframework.data.domain.Pageable.class));
        verify(repository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }
    @Test void consultorNaoListaMesmoSemFiltroHttp() {
        ator(NivelUsuario.CONSULTOR);
        assertThatThrownBy(() -> service.listar(1L, 0, 20)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }
}

package br.com.doutorembalagem.usuario;

import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import br.com.doutorembalagem.usuario.repository.UsuarioPermissaoRepository;
import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
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
class UsuarioSchemaTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioPermissaoRepository usuarioPermissaoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Empresa novaEmpresa() {
        return new Empresa("Embalagens Exemplo Ltda", null, "12345678000195");
    }

    private Usuario novoUsuario(Empresa empresa, String email, NivelUsuario nivel) {
        return new Usuario(empresa, "Joao da Silva", email, "hash-de-teste", nivel, true, true);
    }

    private void inserirUsuarioNativo(String nivel, Long empresaId, String email) {
        entityManager.createNativeQuery("""
                        INSERT INTO usuario (empresa_id, nome, email, senha_hash, nivel, troca_senha_obrigatoria, ativo)
                        VALUES (?, 'Teste Nativo', ?, 'hash-de-teste', ?, false, true)
                        """)
                .setParameter(1, empresaId)
                .setParameter(2, email)
                .setParameter(3, nivel)
                .executeUpdate();
    }

    @Test
    void rejeitaEmailDuplicadoIgnorandoMaiusculasEMinusculas() {
        Empresa empresa = empresaRepository.saveAndFlush(novaEmpresa());
        usuarioRepository.saveAndFlush(novoUsuario(empresa, "joao@exemplo.com", NivelUsuario.ADMIN));

        assertThatThrownBy(() ->
                usuarioRepository.saveAndFlush(novoUsuario(empresa, "JOAO@EXEMPLO.COM", NivelUsuario.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_usuario_email_lower");
    }

    @Test
    void rejeitaEmpresaInexistente() {
        assertThatThrownBy(() -> inserirUsuarioNativo("ADMIN", 999999L, "fantasma@exemplo.com"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("fk_usuario_empresa");
    }

    @Test
    void rejeitaNivelForaDoCatalogo() {
        assertThatThrownBy(() -> inserirUsuarioNativo("OUTRO", null, "outro@exemplo.com"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_usuario_nivel");
    }

    @Test
    void rejeitaSuperadminComEmpresa() {
        Empresa empresa = empresaRepository.saveAndFlush(novaEmpresa());

        assertThatThrownBy(() -> inserirUsuarioNativo("SUPERADMIN", empresa.getId(), "super@exemplo.com"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_usuario_nivel_empresa");
    }

    @Test
    void rejeitaAdminSemEmpresa() {
        assertThatThrownBy(() -> inserirUsuarioNativo("ADMIN", null, "admin@exemplo.com"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("ck_usuario_nivel_empresa");
    }

    @Test
    void impedeExclusaoDaEmpresaComUsuarios() {
        Empresa empresa = empresaRepository.saveAndFlush(novaEmpresa());
        usuarioRepository.saveAndFlush(novoUsuario(empresa, "joao@exemplo.com", NivelUsuario.ADMIN));

        assertThatThrownBy(() -> entityManager.createNativeQuery("DELETE FROM empresa WHERE id = :id")
                        .setParameter("id", empresa.getId())
                        .executeUpdate())
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("fk_usuario_empresa");
    }

    @Test
    void removePermissoesAoExcluirUsuario() {
        Empresa empresa = empresaRepository.saveAndFlush(novaEmpresa());
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario(empresa, "joao@exemplo.com", NivelUsuario.ADMIN));
        usuarioPermissaoRepository.saveAndFlush(new UsuarioPermissao(usuario, Permissao.USUARIOS_LISTAR));
        entityManager.clear();

        Usuario recarregado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        usuarioRepository.delete(recarregado);
        entityManager.flush();

        assertThat(usuarioPermissaoRepository.count()).isZero();
    }

    @Test
    void salvaPermissaoComoTextoDoCatalogo() {
        Empresa empresa = empresaRepository.saveAndFlush(novaEmpresa());
        Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario(empresa, "joao@exemplo.com", NivelUsuario.ADMIN));
        UsuarioPermissao permissao = usuarioPermissaoRepository
                .saveAndFlush(new UsuarioPermissao(usuario, Permissao.USUARIOS_GERIR_PERMISSOES));

        assertThat(permissao.getId().getPermissao()).isEqualTo(Permissao.USUARIOS_GERIR_PERMISSOES);
        assertThat(permissao.getId().getUsuarioId()).isEqualTo(usuario.getId());
        assertThat(usuarioPermissaoRepository.count()).isEqualTo(1);
    }
}

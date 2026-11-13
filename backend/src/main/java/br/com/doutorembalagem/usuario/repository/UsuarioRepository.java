package br.com.doutorembalagem.usuario.repository;

import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Unicidade de email e case-insensitive (indice lower(email) no banco). */
    @Query("select u from Usuario u left join fetch u.empresa where lower(u.email) = lower(:email)")
    Optional<Usuario> buscarPorEmailComEmpresa(@Param("email") String email);

    @Query("select u from Usuario u left join fetch u.empresa where u.id = :id")
    Optional<Usuario> buscarPorIdComEmpresa(@Param("id") Long id);

    boolean existsByNivel(NivelUsuario nivel);

    @EntityGraph(attributePaths = "empresa")
    Page<Usuario> findByEmpresaId(Long empresaId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "empresa")
    Page<Usuario> findAll(Pageable pageable);

    List<UsuarioPermissao> findPermissoesByUsuarioId(Long usuarioId);

    List<UsuarioPermissao> findPermissoesByUsuarioIdIn(java.util.Collection<Long> ids);
}
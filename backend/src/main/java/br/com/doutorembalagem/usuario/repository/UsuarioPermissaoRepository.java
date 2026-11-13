package br.com.doutorembalagem.usuario.repository;

import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import br.com.doutorembalagem.usuario.model.UsuarioPermissaoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioPermissaoRepository extends JpaRepository<UsuarioPermissao, UsuarioPermissaoId> {

    List<UsuarioPermissao> findByIdUsuarioId(Long usuarioId);

    List<UsuarioPermissao> findByIdUsuarioIdIn(java.util.Collection<Long> ids);

}

package br.com.doutorembalagem.usuario.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import br.com.doutorembalagem.usuario.enums.Permissao;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario_permissao")
@Getter
@NoArgsConstructor
public class UsuarioPermissao {

    @EmbeddedId
    private UsuarioPermissaoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public UsuarioPermissao(Usuario usuario, Permissao permissao) {
        this.id = new UsuarioPermissaoId(null, permissao);
        this.usuario = usuario;
    }
}

package br.com.doutorembalagem.usuario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import br.com.doutorembalagem.usuario.enums.Permissao;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UsuarioPermissaoId implements Serializable {

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permissao", length = 80)
    private Permissao permissao;
}

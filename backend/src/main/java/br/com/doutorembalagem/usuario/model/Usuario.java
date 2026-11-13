package br.com.doutorembalagem.usuario.model;

import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @Setter
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Setter
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Setter
    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    private NivelUsuario nivel;

    @Setter
    @Column(name = "troca_senha_obrigatoria", nullable = false)
    private boolean trocaSenhaObrigatoria;

    @Setter
    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Usuario(Empresa empresa, String nome, String email, String senhaHash, NivelUsuario nivel,
            boolean trocaSenhaObrigatoria, boolean ativo) {
        this.empresa = empresa;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.nivel = nivel;
        this.trocaSenhaObrigatoria = trocaSenhaObrigatoria;
        this.ativo = ativo;
    }
}

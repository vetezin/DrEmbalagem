package br.com.doutorembalagem.empresa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "empresa")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "razao_social", nullable = false, length = 200)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Column(name = "cnpj", nullable = false, length = 14)
    private String cnpj;

    @Setter
    @Column(name = "cep", length = 8)
    private String cep;

    @Setter
    @Column(name = "logradouro", length = 200)
    private String logradouro;

    @Setter
    @Column(name = "numero", length = 20)
    private String numero;

    @Setter
    @Column(name = "complemento", length = 120)
    private String complemento;

    @Setter
    @Column(name = "bairro", length = 120)
    private String bairro;

    @Setter
    @Column(name = "cidade", length = 120)
    private String cidade;

    @Setter
    @Column(name = "uf", length = 2)
    private String uf;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    public Empresa(String razaoSocial, String nomeFantasia, String cnpj) {
        this.razaoSocial = razaoSocial;
        this.nomeFantasia = nomeFantasia;
        this.cnpj = cnpj;
    }
}

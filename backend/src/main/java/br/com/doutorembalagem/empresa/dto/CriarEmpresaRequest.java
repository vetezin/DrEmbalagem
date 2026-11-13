package br.com.doutorembalagem.empresa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarEmpresaRequest(
        @NotBlank @Size(max = 200) String razaoSocial,
        @Size(max = 200) String nomeFantasia,
        @NotBlank @Size(max = 18) String cnpj,
        @Pattern(regexp = "[0-9]{8}|[0-9]{5}-[0-9]{3}|^$") String cep,
        @Size(max = 200) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 120) String complemento,
        @Size(max = 120) String bairro,
        @Size(max = 120) String cidade,
        @Pattern(regexp = "[a-zA-Z]{2}|^$") String uf) { }

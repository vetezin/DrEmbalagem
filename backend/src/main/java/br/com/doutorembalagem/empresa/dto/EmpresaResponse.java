package br.com.doutorembalagem.empresa.dto;

import br.com.doutorembalagem.empresa.model.Empresa;

public record EmpresaResponse(Long id, String razaoSocial, String nomeFantasia, String cnpj,
        String cep, String logradouro, String numero, String complemento, String bairro, String cidade, String uf) {
    public static EmpresaResponse fromModel(Empresa e) {
        return new EmpresaResponse(e.getId(), e.getRazaoSocial(), e.getNomeFantasia(), e.getCnpj(),
                e.getCep(), e.getLogradouro(), e.getNumero(), e.getComplemento(), e.getBairro(), e.getCidade(), e.getUf());
    }
}

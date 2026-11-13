package br.com.doutorembalagem.empresa.service;

import br.com.doutorembalagem.empresa.dto.CriarEmpresaRequest;
import br.com.doutorembalagem.empresa.dto.EmpresaResponse;
import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import br.com.doutorembalagem.usuario.dto.PaginaResponse;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.service.AutorizacaoService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmpresaService {
    private final EmpresaRepository repository;
    private final AutorizacaoService autorizacao;

    @Transactional(readOnly = true)
    public PaginaResponse<EmpresaResponse> listar(Long atorId, int page, int size) {
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes ator = autorizacao.atual(atorId);
        PageRequest pedido = PaginaResponse.pedido(page, size);
        if (ator.nivel() == NivelUsuario.CONSULTOR) throw new AccessDeniedException("Acesso negado.");
        Page<Empresa> empresas = ator.nivel() == NivelUsuario.SUPERADMIN
                ? repository.findAll(pedido) : repository.findById(ator.empresaId(), pedido);
        return PaginaResponse.de(empresas.map(EmpresaResponse::fromModel));
    }

    @Transactional
    public EmpresaResponse criar(Long atorId, CriarEmpresaRequest pedido) {
        autorizacao.superadmin(autorizacao.atual(atorId));
        Empresa empresa = new Empresa(pedido.razaoSocial().trim(), texto(pedido.nomeFantasia()), normalizarCnpj(pedido.cnpj()));
        empresa.setCep(texto(pedido.cep()) == null ? null : pedido.cep().replace("-", ""));
        empresa.setLogradouro(texto(pedido.logradouro()));
        empresa.setNumero(texto(pedido.numero()));
        empresa.setComplemento(texto(pedido.complemento()));
        empresa.setBairro(texto(pedido.bairro()));
        empresa.setCidade(texto(pedido.cidade()));
        empresa.setUf(texto(pedido.uf()) == null ? null : pedido.uf().toUpperCase(Locale.ROOT));

        return EmpresaResponse.fromModel(repository.saveAndFlush(empresa));
    }

    /** Receita Federal: manual-dv-cnpj.pdf, ASCII - 48, modulo 11. Valida formato/DV, nao situacao cadastral. */
    public static String normalizarCnpj(String valor) {
        if (valor == null) throw invalidoCnpj();
        String cnpj = valor.trim().toUpperCase(Locale.ROOT);
        if (cnpj.matches("[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/[A-Z0-9]{4}-[0-9]{2}")) {
            cnpj = cnpj.replace(".", "").replace("/", "").replace("-", "");
        }
        if (!cnpj.matches("[A-Z0-9]{12}[0-9]{2}") || cnpj.matches("([0-9])\\1{13}")) throw invalidoCnpj();
        if (digitoCnpj(cnpj.substring(0, 12)) != cnpj.charAt(12) - '0'
                || digitoCnpj(cnpj.substring(0, 13)) != cnpj.charAt(13) - '0') throw invalidoCnpj();
        return cnpj;
    }

    private static int digitoCnpj(String base) {
        int soma = 0;
        for (int i = base.length() - 1, peso = 2; i >= 0; i--, peso = peso == 9 ? 2 : peso + 1) {
            soma += (base.charAt(i) - 48) * peso;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static RequisicaoInvalidaException invalidoCnpj() {
        return new RequisicaoInvalidaException("CNPJ invalido.");
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
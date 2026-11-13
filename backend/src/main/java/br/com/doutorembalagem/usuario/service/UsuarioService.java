package br.com.doutorembalagem.usuario.service;

import br.com.doutorembalagem.autenticacao.service.PoliticaDeSenha;
import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import br.com.doutorembalagem.erro.NaoEncontradoException;
import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import br.com.doutorembalagem.usuario.dto.*;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.*;
import br.com.doutorembalagem.usuario.repository.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final UsuarioPermissaoRepository permissoes;
    private final EmpresaRepository empresas;
    private final AutorizacaoService autorizacao;
    private final PasswordEncoder encoder;

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(Long atorId, int page, int size) {
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes ator = autorizacao.atual(atorId);
        autorizacao.administrar(ator, Permissao.USUARIOS_LISTAR);
        PageRequest pedido = PaginaResponse.pedido(page, size);
        Page<Usuario> resultado = ator.nivel() == NivelUsuario.SUPERADMIN ? usuarios.findAll(pedido)
                : usuarios.findByEmpresaId(ator.empresaId(), pedido);
        Map<Long, Set<Permissao>> porUsuario = new HashMap<>();
        if (!resultado.isEmpty()) {
            usuarios.findPermissoesByUsuarioIdIn(resultado.map(Usuario::getId).getContent()).forEach(p ->
                    porUsuario.computeIfAbsent(p.getId().getUsuarioId(), id -> new HashSet<>()).add(p.getId().getPermissao()));
        }
        return PaginaResponse.de(resultado.map(u -> UsuarioResponse.de(u, porUsuario.getOrDefault(u.getId(), Set.of()))));
    }

    @Transactional
    public UsuarioResponse criar(Long atorId, CriarUsuarioRequest pedido) {
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes ator = autorizacao.atual(atorId);
        autorizacao.administrar(ator, Permissao.USUARIOS_CRIAR);
        autorizacao.conceder(ator, pedido.nivel(), pedido.empresaId(), pedido.permissoes());
        PoliticaDeSenha.validar(pedido.senhaTemporaria());
        String email = emailCanonico(pedido.email());
        Empresa empresa = empresas.findById(pedido.empresaId()).orElseThrow(NaoEncontradoException::new);
        Usuario usuario = usuarios.saveAndFlush(new Usuario(empresa, pedido.nome().trim(), email,
                encoder.encode(pedido.senhaTemporaria()), pedido.nivel(), true, true));
        pedido.permissoes().forEach(p -> permissoes.save(new UsuarioPermissao(usuario, p)));
        permissoes.flush();
        return UsuarioResponse.de(usuario, pedido.permissoes());
    }

    @Transactional
    public UsuarioResponse alterarPermissoes(Long atorId, Long id, AlterarPermissoesRequest pedido) {
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes ator = autorizacao.atual(atorId);
        autorizacao.administrar(ator, Permissao.USUARIOS_GERIR_PERMISSOES);
        Usuario usuario = usuarios.buscarPorIdComEmpresa(id).orElseThrow(NaoEncontradoException::new);
        // Nao revelar se um identificador de outra empresa existe.
        if (ator.nivel() != NivelUsuario.SUPERADMIN && (usuario.getEmpresa() == null
                || !Objects.equals(ator.empresaId(), usuario.getEmpresa().getId()))) throw new NaoEncontradoException();
        autorizacao.conceder(ator, usuario.getNivel(), usuario.getEmpresa() == null ? null : usuario.getEmpresa().getId(), pedido.permissoes());
        List<UsuarioPermissao> atuais = usuarios.findPermissoesByUsuarioId(id);
        Set<Permissao> existentes = atuais.stream().map(p -> p.getId().getPermissao()).collect(Collectors.toSet());
        atuais.stream().filter(p -> !pedido.permissoes().contains(p.getId().getPermissao())).forEach(permissoes::delete);
        pedido.permissoes().stream().filter(p -> !existentes.contains(p))
                .forEach(p -> permissoes.save(new UsuarioPermissao(usuario, p)));
        permissoes.flush();
        return UsuarioResponse.de(usuario, pedido.permissoes());
    }

    @Transactional(readOnly = true)
    public List<String> catalogo(Long atorId) {
        br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes ator = autorizacao.atual(atorId);
        if (ator.nivel() == NivelUsuario.CONSULTOR) throw new AccessDeniedException("Acesso negado.");
        return Arrays.stream(Permissao.values()).map(Enum::name).toList();
    }

    public static String emailCanonico(String email) {
        String valor = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (valor.length() > 254 || !valor.matches("[^\\s@]+@(?:[a-z0-9](?:[a-z0-9\\x2D]{0,61}[a-z0-9])?\\.)+(?:[a-z]{2,63}|xn\\x2D\\x2D[a-z0-9](?:[a-z0-9\\x2D]{0,57}[a-z0-9])?)")) {
            throw new RequisicaoInvalidaException("Email invalido.");
        }
        return valor;
    }
}

package br.com.doutorembalagem.autenticacao.service;

import br.com.doutorembalagem.autenticacao.dto.TrocarSenhaRequest;
import br.com.doutorembalagem.erro.CredenciaisInvalidasException;
import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import br.com.doutorembalagem.erro.SessaoInvalidaException;
import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import br.com.doutorembalagem.usuario.model.UsuarioPermissaoId;

import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/** Casos de uso de autenticacao; sem acesso a HTTP. */
@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AutenticacaoService(AuthenticationManager authenticationManager,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Autentica credenciais; qualquer falha vira o mesmo erro generico. */
    public UsuarioDetalhes autenticar(String email, String senha) {
        if (senha == null || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new CredenciaisInvalidasException();
        }
        try {
            Authentication autenticacao = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email.trim(), senha));
            return (UsuarioDetalhes) autenticacao.getPrincipal();
        } catch (AuthenticationException e) {
            throw new CredenciaisInvalidasException();
        }
    }

    /**
     * Troca obrigatoria/iniciada pelo usuario: valida politica e confirmacao,
     * salva hash e encerra a exigencia na mesma transacao. O chamador libera
     * a sessao somente depois de este metodo retornar (commit concluido).
     */
    @Transactional
    public UsuarioDetalhes trocarSenha(Long idUsuario, TrocarSenhaRequest pedido) {
        PoliticaDeSenha.validar(pedido.novaSenha());
        if (!pedido.novaSenha().equals(pedido.confirmacao())) {
            throw new RequisicaoInvalidaException("Confirmacao de senha nao confere.");
        }
        Usuario usuario = usuarioRepository.buscarPorIdComEmpresa(idUsuario)
                .orElseThrow(SessaoInvalidaException::new);
        if (!usuario.isAtivo()) throw new SessaoInvalidaException();
        if (!usuario.isTrocaSenhaObrigatoria()) {
            throw new RequisicaoInvalidaException("Troca inicial de senha ja concluida.");
        }
        if (passwordEncoder.matches(pedido.novaSenha(), usuario.getSenhaHash())) {
            throw new RequisicaoInvalidaException("Nova senha deve ser distinta da atual.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(pedido.novaSenha()));
        usuario.setTrocaSenhaObrigatoria(false);
        Set<br.com.doutorembalagem.usuario.enums.Permissao> permissoes = usuarioRepository
                .findPermissoesByUsuarioId(usuario.getId()).stream()
                .map(UsuarioPermissao::getId)
                .map(UsuarioPermissaoId::getPermissao)
                .collect(Collectors.toSet());
        return UsuarioDetalhes.de(usuario, permissoes);
    }
}

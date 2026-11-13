package br.com.doutorembalagem.autenticacao.controller;

import br.com.doutorembalagem.autenticacao.dto.CsrfResponse;
import br.com.doutorembalagem.autenticacao.dto.LoginRequest;
import br.com.doutorembalagem.autenticacao.dto.SessaoResponse;
import br.com.doutorembalagem.autenticacao.dto.TrocarSenhaRequest;
import br.com.doutorembalagem.autenticacao.service.AutenticacaoService;
import br.com.doutorembalagem.autenticacao.service.ServicoTentativasLogin;
import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.usuario.service.ConsultaUsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ciclo de sessao: emissao de CSRF, login com rotacao de id de sessao,
 * identidade, troca de senha e logout. Sem regra de negocio aqui.
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final ServicoTentativasLogin tentativas;
    private final ConsultaUsuarioService consultaUsuarioService;
    private final SecurityContextRepository contextoRepository;
    private final SessionAuthenticationStrategy rotacaoDeSessao;
    private final CsrfTokenRepository csrfTokenRepository;

    public AutenticacaoController(AutenticacaoService autenticacaoService,
            ServicoTentativasLogin tentativas,
            ConsultaUsuarioService consultaUsuarioService,
            SessionAuthenticationStrategy rotacaoDeSessao,
            CsrfTokenRepository csrfTokenRepository) {
        this.autenticacaoService = autenticacaoService;
        this.tentativas = tentativas;
        this.consultaUsuarioService = consultaUsuarioService;
        this.contextoRepository = new HttpSessionSecurityContextRepository();
        this.rotacaoDeSessao = rotacaoDeSessao;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        token.getToken();
        return CsrfResponse.de(token);
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest pedido,
            HttpServletRequest request, HttpServletResponse response) {
        String chave = pedido.email().trim().toLowerCase();
        tentativas.verificarIp(request.getRemoteAddr());
        tentativas.verificarBloqueio(chave);
        try {
            UsuarioDetalhes detalhes = autenticacaoService.autenticar(pedido.email(), pedido.senha());
            tentativas.registrarSucesso(chave);
            rotacaoDeSessao.onAuthentication(
                    autenticacaoComIdentidade(detalhes), request, response);
            renovarContexto(detalhes, request, response);
            csrfTokenRepository.saveToken(null, request, response);
            return detalhes.paraSessao();
        } catch (RuntimeException e) {
            tentativas.registrarFalha(chave);
            throw e;
        }
    }

    @GetMapping("/me")
    public SessaoResponse me() {
        UsuarioDetalhes detalhes = detalhesAtuais();
        return consultaUsuarioService.buscarPorId(detalhes.id()).paraSessao();
    }

    @PostMapping("/trocar-senha")
    public SessaoResponse trocarSenha(@Valid @RequestBody TrocarSenhaRequest pedido,
            HttpServletRequest request, HttpServletResponse response) {
        UsuarioDetalhes detalhes = detalhesAtuais();
        UsuarioDetalhes atualizado = autenticacaoService.trocarSenha(detalhes.id(), pedido);
        rotacaoDeSessao.onAuthentication(autenticacaoComIdentidade(atualizado), request, response);
        renovarContexto(atualizado, request, response);
        csrfTokenRepository.saveToken(null, request, response);
        return atualizado.paraSessao();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, autenticacao);
    }

    private UsuarioDetalhes detalhesAtuais() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null
                && autenticacao.getPrincipal() instanceof UsuarioDetalhes detalhes) {
            return detalhes;
        }
        throw new br.com.doutorembalagem.erro.SessaoInvalidaException();
    }

    private void renovarContexto(UsuarioDetalhes detalhes,
            HttpServletRequest request, HttpServletResponse response) {
        Authentication autenticacao = autenticacaoComIdentidade(detalhes);
        org.springframework.security.core.context.SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
        contextoRepository.saveContext(contexto, request, response);
    }

    private Authentication autenticacaoComIdentidade(UsuarioDetalhes detalhes) {
        return org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                .authenticated(detalhes, null, detalhes.getAuthorities());
    }
}

package br.com.doutorembalagem.autenticacao.config;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetalhes;
import br.com.doutorembalagem.erro.SessaoInvalidaException;
import br.com.doutorembalagem.usuario.service.ConsultaUsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/** Revoga conta inativa, senha substituida e permissoes antigas antes de autorizar cada requisicao. */
final class AtualizarIdentidadeFilter extends OncePerRequestFilter {
    private final ConsultaUsuarioService consulta;

    AtualizarIdentidadeFilter(ConsultaUsuarioService consulta) { this.consulta = consulta; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        org.springframework.security.core.Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioDetalhes anterior) {
            try {
                UsuarioDetalhes atual = consulta.buscarPorId(anterior.id());
                if (!atual.senhaHash().equals(anterior.senhaHash())) throw new SessaoInvalidaException();
                org.springframework.security.core.context.SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(atual, null, atual.getAuthorities()));
                SecurityContextHolder.setContext(contexto);
            } catch (SessaoInvalidaException e) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                new EntradaNaoAutenticada().escrever(response, 401, "Autenticacao necessaria.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

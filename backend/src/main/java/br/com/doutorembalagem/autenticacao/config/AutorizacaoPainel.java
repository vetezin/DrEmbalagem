package br.com.doutorembalagem.autenticacao.config;

import java.util.function.Supplier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Regra de qualquer rota fora de /api/auth: exige sessao autenticada
 * (anomima -> negado -> entry point 401) e bloqueia o acesso ao painel
 * enquanto a troca de senha inicial nao for concluida (403).
 */
public class AutorizacaoPainel implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String TROCA_SENHA_PENDENTE = "TROCA_SENHA_PENDENTE";

    @Override
    public AuthorizationDecision authorize(Supplier<? extends Authentication> autenticacao,
            RequestAuthorizationContext contexto) {
        Authentication atual = autenticacao.get();
        if (atual != null && atual.getAuthorities().stream()
                .anyMatch(autoridade -> TROCA_SENHA_PENDENTE.equals(autoridade.getAuthority()))) {
            return new AuthorizationDecision(false);
        }
        boolean autenticado = atual != null
                && atual.isAuthenticated()
                && !(atual instanceof AnonymousAuthenticationToken);
        return new AuthorizationDecision(autenticado);
    }
}

package br.com.doutorembalagem.autenticacao.dto;

import org.springframework.security.web.csrf.CsrfToken;

/** Token CSRF em GET /api/auth/csrf; o valor fica na sessao do servidor. */
public record CsrfResponse(String headerName, String parameterName, String token) {

    public static CsrfResponse de(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
    }
}

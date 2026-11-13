package br.com.doutorembalagem.autenticacao.config;

import br.com.doutorembalagem.erro.ApiErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Resposta 403 JSON para acessos negados, incluindo falha de CSRF
 * (o CsrfFilter chama este handler diretamente, mesmo sem sessao).
 */
public class AcessoNegadoHandler implements AccessDeniedHandler {

    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException e) throws IOException {
        String mensagem = (e instanceof CsrfException)
                ? "Token CSRF ausente ou invalido."
                : "Acesso negado.";
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(new ApiErro(mensagem)));
    }
}

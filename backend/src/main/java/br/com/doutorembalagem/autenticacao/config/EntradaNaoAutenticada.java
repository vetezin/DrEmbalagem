package br.com.doutorembalagem.autenticacao.config;

import br.com.doutorembalagem.erro.ApiErro;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/** Resposta 401 JSON para requisicoes sem sessao. */
public class EntradaNaoAutenticada implements AuthenticationEntryPoint {

    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException e) throws IOException {
        escrever(response, HttpServletResponse.SC_UNAUTHORIZED, "Autenticacao necessaria.");
    }

    void escrever(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(json.writeValueAsString(new ApiErro(mensagem)));
    }
}

package br.com.doutorembalagem.autenticacao;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP do JDK com armazenamento real de cookies, para provar
 * comportamento de sessao e CSRF sobre servidor HTTP real.
 */
final class ClienteHttpTeste implements AutoCloseable {

    record Resposta(int status, String corpo, Map<String, List<String>> cabecalhos) {
    }

    private final HttpClient cliente;
    private final CookieManager cookies;
    private final String base;

    ClienteHttpTeste(String base) {
        CookieManager gerenciador = new CookieManager();
        gerenciador.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ALL);
        this.cliente = HttpClient.newBuilder().cookieHandler(gerenciador).build();
        this.cookies = gerenciador;
        this.base = base;
    }

    /** Valor atual do cookie de sessao armazenado, se existir. */
    String cookieJsessionId() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("JSESSIONID"))
                .map(java.net.HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    Resposta get(String caminho) {
        return enviar("GET", caminho, null, null);
    }

    Resposta post(String caminho, String corpoJson) {
        return enviar("POST", caminho, corpoJson, null);
    }

    Resposta post(String caminho, String corpoJson, Map<String, String> cabecalhosExtras) {
        return enviar("POST", caminho, corpoJson, cabecalhosExtras);
    }

    Resposta postComCsrf(String caminho, String corpoJson, String tokenCsrf) {
        return post(caminho, corpoJson, Map.of("X-CSRF-TOKEN", tokenCsrf));
    }

    private Resposta enviar(String metodo, String caminho, String corpoJson,
            Map<String, String> cabecalhosExtras) {
        HttpRequest.Builder pedido = HttpRequest.newBuilder()
                .uri(URI.create(base + caminho))
                .header("Content-Type", "application/json");
        if (cabecalhosExtras != null) {
            cabecalhosExtras.forEach(pedido::header);
        }
        try {
            HttpResponse<byte[]> resposta = cliente.send(
                    pedido.method(metodo, corpoJson == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(corpoJson)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            return new Resposta(resposta.statusCode(),
                    resposta.body() == null ? "" : new String(resposta.body(), java.nio.charset.StandardCharsets.UTF_8),
                    resposta.headers().map());
        } catch (Exception e) {
            throw new IllegalStateException("Falha na chamada HTTP de teste: " + caminho, e);
        }
    }

    @Override
    public void close() {
        // HttpClient was designed to be long-lived; no explicit close needed.
    }
}

package br.com.doutorembalagem.autenticacao;

import br.com.doutorembalagem.empresa.model.Empresa;
import br.com.doutorembalagem.empresa.repository.EmpresaRepository;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.enums.Permissao;
import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.model.UsuarioPermissao;
import br.com.doutorembalagem.usuario.repository.UsuarioPermissaoRepository;
import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Disabled("Integracao bloqueada: requer banco descartavel isolado e revisao da limpeza antes de habilitar.")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("database")
class AutenticacaoHttpTest {

    private static final String SENHA_TEMPORARIA = "Temp123!";
    private static final String SENHA_NOVA = "Nova456#";
    private static final String MSG_CREDENCIAIS_INVALIDAS = "Credenciais invalidas.";

    @LocalServerPort
    private int porta;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioPermissaoRepository usuarioPermissaoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void bancoDeTeste(DynamicPropertyRegistry registro) {
        String url = System.getenv("EMBALAGEM_TEST_DB_URL");
        String usuario = System.getenv("EMBALAGEM_TEST_DB_USERNAME");
        String senha = System.getenv("EMBALAGEM_TEST_DB_PASSWORD");
        if (url == null || !url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/embalagem_test_isolado")
                || !"CONFIRMO_BANCO_DESCARTAVEL".equals(System.getenv("EMBALAGEM_TEST_DESTRUCTIVE_OPT_IN"))
                || usuario == null || senha == null) {
            throw new IllegalStateException(
                    "Variaveis EMBALAGEM_TEST_DB_URL, EMBALAGEM_TEST_DB_USERNAME e "
                            + "EMBALAGEM_TEST_DB_PASSWORD devem estar definidas.");
        }
        // Somente banco local descartavel com autorizacao destrutiva explicita; classe segue desabilitada.
        registro.add("spring.datasource.url", () -> url);
        registro.add("spring.datasource.username", () -> usuario);
        registro.add("spring.datasource.password", () -> senha);
    }

    @AfterEach
    void limparBancoDeTeste() {
        String url;
        try (Connection c = dataSource.getConnection()) {
            url = c.getMetaData().getURL();
        } catch (Exception e) {
            throw new DataAccessResourceFailureException("Falha ao abrir conexao de teste", e);
        }
        if (!url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/embalagem_test_isolado")
                || !"CONFIRMO_BANCO_DESCARTAVEL".equals(System.getenv("EMBALAGEM_TEST_DESTRUCTIVE_OPT_IN"))) {
            throw new IllegalStateException("Limpeza recusada fora do banco descartavel explicitamente autorizado.");
        }
        executar("""
                DELETE FROM usuario_permissao;
                DELETE FROM usuario;
                DELETE FROM empresa;
                """);
    }

    private void executar(String sql) {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao executar SQL de limpeza do banco de teste", e);
        }
    }

    private ClienteHttpTeste cliente() {
        return new ClienteHttpTeste("http://127.0.0.1:" + porta);
    }

    private String tokenCsrf(ClienteHttpTeste cliente) throws Exception {
        ClienteHttpTeste.Resposta resposta = cliente.get("/api/auth/csrf");
        assertThat(resposta.status()).isEqualTo(200);
        return json.readTree(resposta.corpo()).path("token").asText();
    }

    private Empresa criarEmpresa(String cnpj) {
        return empresaRepository.save(new Empresa("Embalagens Exemplo Ltda", null, cnpj));
    }

    private Usuario criarUsuario(Empresa empresa, String email, NivelUsuario nivel, String senha,
            boolean trocaObrigatoria, boolean ativo, Set<Permissao> permissoes) {
        Usuario usuario = new Usuario(empresa, "Usuario Teste", email,
                passwordEncoder.encode(senha), nivel, trocaObrigatoria, ativo);
        Usuario salvo = usuarioRepository.save(usuario);
        for (Permissao permissao : permissoes) {
            usuarioPermissaoRepository.save(new UsuarioPermissao(salvo, permissao));
        }
        return salvo;
    }

    // ===================== Testes =====================

    @Test
    void loginInvalidoRetorna401ComMensagemGenerica() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "joao@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, false, true,
                Set.of());
        try (ClienteHttpTeste cliente = cliente()) {
            String token = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta resposta = cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"joao@exemplo.com\",\"senha\":\"Errada123!\"}", token);
            assertThat(resposta.status()).isEqualTo(401);
            assertThat(json.readTree(resposta.corpo()).path("mensagem").asText())
                    .isEqualTo(MSG_CREDENCIAIS_INVALIDAS);
        }
    }

    @Test
    void loginDeEmailInexistenteRetornaMesmaMensagemDeCredencialInvalida() throws Exception {
        try (ClienteHttpTeste cliente = cliente()) {
            String token = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta resposta = cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"fantasma@exemplo.com\",\"senha\":\"Abcdef12!\"}", token);
            assertThat(resposta.status()).isEqualTo(401);
            assertThat(json.readTree(resposta.corpo()).path("mensagem").asText())
                    .isEqualTo(MSG_CREDENCIAIS_INVALIDAS);
        }
    }

    @Test
    void contaInativaRetornaMesmaMensagemDeCredencialInvalida() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "inativo@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, false,
                false, Set.of());
        try (ClienteHttpTeste cliente = cliente()) {
            String token = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta resposta = cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"inativo@exemplo.com\",\"senha\":\"Temp123!\"}", token);
            assertThat(resposta.status()).isEqualTo(401);
            assertThat(json.readTree(resposta.corpo()).path("mensagem").asText())
                    .isEqualTo(MSG_CREDENCIAIS_INVALIDAS);
        }
    }

    @Test
    void loginValidoAutenticaRotacionaSessaoERetornaIdentidadeSemHash() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "admin@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, false, true,
                Set.of(Permissao.USUARIOS_LISTAR));
        try (ClienteHttpTeste cliente = cliente()) {
            String token = tokenCsrf(cliente);
            String cookieAntes = cliente.cookieJsessionId();

            ClienteHttpTeste.Resposta resposta = cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"ADMIN@EXEMPLO.COM\",\"senha\":\"Temp123!\"}", token);

            assertThat(resposta.status()).isEqualTo(200);
            JsonNode corpo = json.readTree(resposta.corpo());
            assertThat(corpo.path("email").asText()).isEqualTo("admin@exemplo.com");
            assertThat(corpo.path("nivel").asText()).isEqualTo("ADMIN");
            assertThat(corpo.path("trocaSenhaObrigatoria").asBoolean()).isFalse();
            assertThat(corpo.has("senhaHash")).isFalse();
            assertThat(corpo.path("permissoes").size()).isEqualTo(1);

            String cookieDepois = cliente.cookieJsessionId();
            assertThat(cookieDepois).as("ID de sessao deve mudar no login").isNotEqualTo(cookieAntes);
        }
    }

    @Test
    void loginValidoDefineCookieDeSessaoHttpOnly() throws Exception {
        try (ClienteHttpTeste cliente = cliente()) {
            ClienteHttpTeste.Resposta resposta = cliente.get("/api/auth/csrf");
            assertThat(resposta.status()).isEqualTo(200);
            List<String> setCookie = resposta.cabecalhos().get("Set-Cookie");
            assertThat(setCookie).isNotNull();
            assertThat(setCookie.get(0)).contains("JSESSIONID", "HttpOnly", "SameSite=Lax");
        }
    }

    @Test
    void postSemCsrfRetorna403() throws Exception {
        try (ClienteHttpTeste cliente = cliente()) {
            cliente.get("/api/auth/csrf");
            ClienteHttpTeste.Resposta resposta = cliente.post("/api/auth/login",
                    "{\"email\":\"x@exemplo.com\",\"senha\":\"Abcdef12!\"}");
            assertThat(resposta.status()).isEqualTo(403);
        }
    }

    @Test
    void usuarioComTrocaObrigatoriaSoAcessaAuth() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "novo@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, true, true,
                Set.of(Permissao.USUARIOS_LISTAR));
        try (ClienteHttpTeste cliente = cliente()) {
            String token = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta login = cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"novo@exemplo.com\",\"senha\":\"Temp123!\"}", token);
            assertThat(login.status()).isEqualTo(200);
            assertThat(json.readTree(login.corpo()).path("trocaSenhaObrigatoria").asBoolean()).isTrue();

            assertThat(cliente.get("/api/auth/me").status()).isEqualTo(200);
            ClienteHttpTeste.Resposta gerir = cliente.get("/api/test/gerir");
            assertThat(gerir.status()).as("painel bloqueado ate trocar senha").isEqualTo(403);
        }
    }

    @Test
    void trocaSenhaFracaConfirmacaoDivergenteEIgualATemporariaSaoRejeitadas() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "novo2@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, true, true,
                Set.of());
        try (ClienteHttpTeste cliente = cliente()) {
            String token1 = tokenCsrf(cliente);
            cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"novo2@exemplo.com\",\"senha\":\"Temp123!\"}", token1);

            String token2 = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta fraca = cliente.postComCsrf("/api/auth/trocar-senha",
                    "{\"novaSenha\":\"abc1!\",\"confirmacao\":\"abc1!\"}", token2);
            assertThat(fraca.status()).isEqualTo(400);

            ClienteHttpTeste.Resposta divergente = cliente.postComCsrf("/api/auth/trocar-senha",
                    "{\"novaSenha\":\"" + SENHA_NOVA + "\",\"confirmacao\":\"Outra789#\"}", token2);
            assertThat(divergente.status()).isEqualTo(400);

            ClienteHttpTeste.Resposta igualTemporaria = cliente.postComCsrf("/api/auth/trocar-senha",
                    "{\"novaSenha\":\"" + SENHA_TEMPORARIA + "\",\"confirmacao\":\"" + SENHA_TEMPORARIA
                            + "\"}", token2);
            assertThat(igualTemporaria.status()).isEqualTo(400);

            assertThat(cliente.get("/api/test/gerir").status()).as("ainda bloqueado").isEqualTo(403);
        }
    }

    @Test
    void trocaSenhaValidaSalvaHashLiberaPainelEAntigaDeixaDeServir() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        Usuario usuario = criarUsuario(empresa, "novo3@exemplo.com", NivelUsuario.ADMIN,
                SENHA_TEMPORARIA, true, true, Set.of(Permissao.USUARIOS_GERIR_PERMISSOES));
        try (ClienteHttpTeste cliente = cliente()) {
            String token1 = tokenCsrf(cliente);
            cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"novo3@exemplo.com\",\"senha\":\"Temp123!\"}", token1);

            String token2 = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta troca = cliente.postComCsrf("/api/auth/trocar-senha",
                    "{\"novaSenha\":\"" + SENHA_NOVA + "\",\"confirmacao\":\"" + SENHA_NOVA + "\"}",
                    token2);
            assertThat(troca.status()).isEqualTo(200);
            assertThat(json.readTree(troca.corpo()).path("trocaSenhaObrigatoria").asBoolean()).isFalse();

            assertThat(passwordEncoder.matches(SENHA_NOVA,
                    usuarioRepository.findById(usuario.getId()).orElseThrow().getSenhaHash())).isTrue();
            assertThat(cliente.get("/api/test/gerir").status()).as("painel liberado").isEqualTo(200);
        }
    }

    @Test
    void logoutInvalidaSessaoEContexto() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "sai@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, false, true,
                Set.of());
        try (ClienteHttpTeste cliente = cliente()) {
            String token1 = tokenCsrf(cliente);
            assertThat(cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"sai@exemplo.com\",\"senha\":\"Temp123!\"}", token1).status())
                    .isEqualTo(200);

            String token2 = tokenCsrf(cliente);
            ClienteHttpTeste.Resposta logout = cliente.postComCsrf("/api/auth/logout", null, token2);
            assertThat(logout.status()).isEqualTo(204);

            assertThat(cliente.get("/api/auth/me").status()).as("sessao encerrada").isEqualTo(401);
        }
    }

    @Test
    void meRetornaIdentidadeSemHash() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "me@exemplo.com", NivelUsuario.ADMIN, SENHA_TEMPORARIA, false, true,
                Set.of(Permissao.USUARIOS_CRIAR));
        try (ClienteHttpTeste cliente = cliente()) {
            String token1 = tokenCsrf(cliente);
            cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"me@exemplo.com\",\"senha\":\"Temp123!\"}", token1);
            ClienteHttpTeste.Resposta me = cliente.get("/api/auth/me");
            assertThat(me.status()).isEqualTo(200);
            JsonNode corpo = json.readTree(me.corpo());
            assertThat(corpo.path("email").asText()).isEqualTo("me@exemplo.com");
            assertThat(corpo.path("permissoes").size()).isEqualTo(1);
            assertThat(corpo.has("senhaHash")).isFalse();
        }
    }

    @Test
    void endpointAdministrativoSemSessaoRetorna401Json() throws Exception {
        try (ClienteHttpTeste cliente = cliente()) {
            ClienteHttpTeste.Resposta resposta = cliente.get("/api/test/gerir");
            assertThat(resposta.status()).isEqualTo(401);
            assertThat(json.readTree(resposta.corpo()).has("mensagem")).isTrue();
        }
    }

    @Test
    void consultorComPermissaoSemAcessoAoGerirRetorna403() throws Exception {
        Empresa empresa = criarEmpresa("12345678000195");
        criarUsuario(empresa, "consultor@exemplo.com", NivelUsuario.CONSULTOR, SENHA_TEMPORARIA,
                false, true, Set.of(Permissao.USUARIOS_LISTAR));
        try (ClienteHttpTeste cliente = cliente()) {
            String token1 = tokenCsrf(cliente);
            cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"consultor@exemplo.com\",\"senha\":\"Temp123!\"}", token1);
            assertThat(cliente.get("/api/test/gerir").status()).isEqualTo(403);
        }
    }

    @Test
    void superadminTemAcessoGlobalSemLinhasDePermissao() throws Exception {
        criarUsuario(null, "super@exemplo.com", NivelUsuario.SUPERADMIN, SENHA_TEMPORARIA, false,
                true, Set.of());
        try (ClienteHttpTeste cliente = cliente()) {
            String token1 = tokenCsrf(cliente);
            cliente.postComCsrf("/api/auth/login",
                    "{\"email\":\"super@exemplo.com\",\"senha\":\"Temp123!\"}", token1);
            assertThat(cliente.get("/api/test/gerir").status()).isEqualTo(200);
        }
    }

    @TestConfiguration
    static class EndpointDeProva {
        @RestController
        static class Prova {
            /** Convencao de autorizacao fina da Task 3: anotacao por metodo no controller. */
            @GetMapping("/api/test/gerir")
            @PreAuthorize("hasAuthority('PERM_USUARIOS_GERIR_PERMISSOES') or hasRole('SUPERADMIN')")
            String gerir() {
                return "ok";
            }
        }
    }
}

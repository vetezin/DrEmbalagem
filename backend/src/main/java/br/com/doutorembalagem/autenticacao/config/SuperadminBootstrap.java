package br.com.doutorembalagem.autenticacao.config;

import br.com.doutorembalagem.autenticacao.service.PoliticaDeSenha;
import br.com.doutorembalagem.erro.RequisicaoInvalidaException;
import br.com.doutorembalagem.usuario.service.UsuarioService;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.doutorembalagem.usuario.enums.NivelUsuario;
import br.com.doutorembalagem.usuario.model.Usuario;
import br.com.doutorembalagem.usuario.repository.UsuarioRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstrap explicito e idempotente do primeiro SUPERADMIN: executado somente
 * com app.superadmin-bootstrap.enabled=true, le credenciais de variaveis de
 * ambiente (nunca de arquivo versionado, default ou log) e nao altera nada se
 * ja existir algum SUPERADMIN. Nunca registra senha ou hash.
 */
@Component
@ConditionalOnProperty(name = "app.superadmin-bootstrap.enabled", havingValue = "true")
public class SuperadminBootstrap implements ApplicationRunner {

    private static final Log log = LogFactory.getLog(SuperadminBootstrap.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Function<String, String> ambiente;

    @Autowired
    public SuperadminBootstrap(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this(usuarioRepository, passwordEncoder, System::getenv);
    }

    SuperadminBootstrap(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
            Function<String, String> ambiente) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.ambiente = ambiente;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = ambiente.apply("APP_SUPERADMIN_EMAIL");
        String senha = ambiente.apply("APP_SUPERADMIN_SENHA");
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap do SUPERADMIN exige APP_SUPERADMIN_EMAIL e APP_SUPERADMIN_SENHA no ambiente.");
        }
        if (usuarioRepository.existsByNivel(NivelUsuario.SUPERADMIN)) {
            log.info("Ja existe SUPERADMIN; bootstrap nao alterou nada.");
            return;
        }
        String nome = ambiente.apply("APP_SUPERADMIN_NOME");
        nome = nome == null ? "Superadmin" : nome.trim();
        if (nome.isBlank() || nome.length() > 120) {
            throw new RequisicaoInvalidaException("Nome deve ter entre 1 e 120 caracteres.");
        }
        PoliticaDeSenha.validar(senha);
        usuarioRepository.save(new Usuario(null, nome, UsuarioService.emailCanonico(email),
                passwordEncoder.encode(senha), NivelUsuario.SUPERADMIN, true, true));
        log.info("SUPERADMIN criado com troca de senha obrigatoria no primeiro acesso.");
    }
}

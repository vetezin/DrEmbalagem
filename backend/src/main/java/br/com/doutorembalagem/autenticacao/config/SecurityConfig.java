package br.com.doutorembalagem.autenticacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import br.com.doutorembalagem.autenticacao.service.UsuarioDetailsService;

/**
 * Sessao em cookie HttpOnly com CSRF por token na sessao; login manual
 * persiste o contexto com SecurityContextRepository e rotaciona o id de sessao.
 * Autorizacao fina usa @PreAuthorize com PERM_<permissao> ou ROLE_SUPERADMIN.
 * CORS tratado pelo proxy Vite (frontend -> backend). Nenhuma configuracao CORS no Java.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(UsuarioDetailsService usuarioDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(usuarioDetailsService);
        provedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provedor);
    }

    /** Rotacao do id de sessao aplicada manualmente no login (sem filtro de formulario). */
    @Bean
    public SessionAuthenticationStrategy sessaoAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            br.com.doutorembalagem.usuario.service.ConsultaUsuarioService consulta) throws Exception {
        http
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .securityContext(contexto -> contexto.requireExplicitSave(true))
                .addFilterBefore(new AtualizarIdentidadeFilter(consulta),
                        org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .authorizeHttpRequests(autorizar -> autorizar
                        .requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/trocar-senha", "/api/auth/logout")
                        .authenticated()
                        .anyRequest().access(new AutorizacaoPainel()))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(new EntradaNaoAutenticada())
                        .accessDeniedHandler(new AcessoNegadoHandler()));
        return http.build();
    }
}
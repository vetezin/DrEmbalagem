package br.com.doutorembalagem.erro;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/**
 * Conversao central de erros em JSON {"mensagem": "..."} com mensagens genericas,
 * sem stack trace, SQL ou classes internas.
 */
@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErro> requisicaoInvalida(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(new ApiErro("Requisicao invalida."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErro> corpoIlegivel(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(new ApiErro("Requisicao invalida."));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ApiErro> requisicaoInvalida(RequisicaoInvalidaException e) {
        return ResponseEntity.badRequest().body(new ApiErro(e.getMessage()));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ApiErro> credenciaisInvalidas(CredenciaisInvalidasException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErro("Credenciais invalidas."));
    }

    @ExceptionHandler(SessaoInvalidaException.class)
    public ResponseEntity<ApiErro> sessaoInvalida(SessaoInvalidaException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErro("Autenticacao necessaria."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErro> erroDeAutenticacao(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErro("Autenticacao necessaria."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErro> erroDeAutorizacao(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiErro("Acesso negado."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErro> conflito(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErro("Conflito com dados existentes."));
    }

    @ExceptionHandler(LimiteTentativasException.class)
    public ResponseEntity<ApiErro> limiteTentativas(LimiteTentativasException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ApiErro(e.getMessage()));
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ApiErro> naoEncontrado(NaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErro("Recurso nao encontrado."));
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ApiErro> parametroInvalido(Exception e) {
        return ResponseEntity.badRequest().body(new ApiErro("Requisicao invalida."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErro> erroInterno(Exception e) {
        return ResponseEntity.internalServerError().body(new ApiErro("Erro interno."));
    }
}

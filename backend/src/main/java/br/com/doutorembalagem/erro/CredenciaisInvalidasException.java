package br.com.doutorembalagem.erro;

/** Falha de autenticacao; a mensagem exposta ao cliente e fixa e generica. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Credenciais invalidas.");
    }
}

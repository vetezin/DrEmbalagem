package br.com.doutorembalagem.erro;

/** Sessao referenciada nao existe mais ou perdeu autenticacao. */
public class SessaoInvalidaException extends RuntimeException {

    public SessaoInvalidaException() {
        super("Autenticacao necessaria.");
    }
}

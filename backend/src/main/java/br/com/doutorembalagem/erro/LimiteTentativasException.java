package br.com.doutorembalagem.erro;

public class LimiteTentativasException extends RuntimeException {
    public LimiteTentativasException(String mensagem) {
        super(mensagem);
    }
}
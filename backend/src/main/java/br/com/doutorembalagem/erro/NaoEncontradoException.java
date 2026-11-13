package br.com.doutorembalagem.erro;

public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException() { super("Recurso nao encontrado."); }
}

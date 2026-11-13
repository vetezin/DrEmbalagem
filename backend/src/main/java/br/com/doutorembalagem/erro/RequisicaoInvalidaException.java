package br.com.doutorembalagem.erro;

/** Erro de entrada do cliente; mensagem e texto escrito pelo proprio codigo. */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}

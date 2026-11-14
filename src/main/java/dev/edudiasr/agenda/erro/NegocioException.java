package dev.edudiasr.agenda.erro;

import org.springframework.http.HttpStatus;

/** Regra de negócio quebrada. A mensagem vai pro cliente da API, então é escrita pra ele. */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;

    public NegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static NegocioException naoEncontrado(String mensagem) {
        return new NegocioException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static NegocioException conflito(String mensagem) {
        return new NegocioException(HttpStatus.CONFLICT, mensagem);
    }

    public static NegocioException invalido(String mensagem) {
        return new NegocioException(HttpStatus.UNPROCESSABLE_CONTENT, mensagem);
    }
}

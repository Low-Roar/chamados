package br.com.sistemas.chamados.exception;

/** Lançada ao tentar alterar ou atribuir técnico a um chamado FECHADO (vira HTTP 409). */
public class ChamadoFechadoException extends RuntimeException {
    public ChamadoFechadoException(String mensagem) {
        super(mensagem);
    }
}

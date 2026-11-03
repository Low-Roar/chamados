package br.com.sistemas.chamados.entity;

public enum StatusChamado {
    ABERTO,
    EM_ATENDIMENTO,
    AGUARDANDO_CLIENTE,
    FECHADO;

    /**
     * Transições válidas:
     * ABERTO -> EM_ATENDIMENTO | FECHADO
     * EM_ATENDIMENTO -> AGUARDANDO_CLIENTE | FECHADO
     * AGUARDANDO_CLIENTE -> EM_ATENDIMENTO | FECHADO
     * FECHADO -> (nenhuma)
     */
    public boolean podeIrPara(StatusChamado destino) {
        return switch (this) {
            case ABERTO -> destino == EM_ATENDIMENTO || destino == FECHADO;
            case EM_ATENDIMENTO -> destino == AGUARDANDO_CLIENTE || destino == FECHADO;
            case AGUARDANDO_CLIENTE -> destino == EM_ATENDIMENTO || destino == FECHADO;
            case FECHADO -> false;
        };
    }
}

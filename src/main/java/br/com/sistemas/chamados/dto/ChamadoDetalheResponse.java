package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

/** Resposta do GET /chamados/{id}: os dados do chamado mais os comentários. */
public record ChamadoDetalheResponse(
    Long id,
    String titulo,
    String descricao,
    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,
    Prioridade prioridade,
    StatusChamado status,
    Long clienteId,
    String clienteNome,
    Long categoriaId,
    String categoriaNome,
    Long tecnicoId,
    String tecnicoNome,
    List<ComentarioResponse> comentarios
) {
    public static ChamadoDetalheResponse de(Chamado c, List<Comentario> comentarios) {
        ChamadoResponse r = ChamadoResponse.de(c);
        return new ChamadoDetalheResponse(
            r.id(), r.titulo(), r.descricao(), r.dataAbertura(), r.dataFechamento(),
            r.prioridade(), r.status(), r.clienteId(), r.clienteNome(),
            r.categoriaId(), r.categoriaNome(), r.tecnicoId(), r.tecnicoNome(),
            comentarios.stream().map(ComentarioResponse::de).toList());
    }
}

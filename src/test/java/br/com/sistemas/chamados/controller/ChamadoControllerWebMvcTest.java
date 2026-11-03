package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ComentarioService;

@WebMvcTest(ChamadoController.class)
class ChamadoControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ChamadoService service;

    @MockBean
    private ComentarioService comentarioService;

    private static final String VALIDO =
        "{\"titulo\":\"Sem internet\",\"descricao\":\"A rede caiu\",\"prioridade\":\"ALTA\","
        + "\"clienteId\":1,\"categoriaId\":1}";

    private static ChamadoResponse resposta(StatusChamado status) {
        return new ChamadoResponse(1L, "Sem internet", "A rede caiu", LocalDateTime.now(), null,
            Prioridade.ALTA, status, 1L, "Ana", 1L, "Rede", null, null);
    }

    @Test
    void criarRetorna201() throws Exception {
        when(service.criar(any(ChamadoRequest.class))).thenReturn(resposta(StatusChamado.ABERTO));

        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void criarComDadosInvalidosRetorna400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"\",\"descricao\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes").isNotEmpty());
    }

    @Test
    void prioridadeInexistenteRetorna400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content(VALIDO.replace("ALTA", "URGENTISSIMA")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(eq(99999L)))
            .thenThrow(new RecursoNaoEncontradoException("Chamado 99999 não encontrado"));

        mvc.perform(get("/chamados/99999")).andExpect(status().isNotFound());
    }

    @Test
    void transicaoInvalidaRetorna409() throws Exception {
        when(service.alterarStatus(eq(1L), eq(StatusChamado.AGUARDANDO_CLIENTE)))
            .thenThrow(new TransicaoStatusInvalidaException(
                "Transição de status inválida: ABERTO -> AGUARDANDO_CLIENTE"));

        mvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"AGUARDANDO_CLIENTE\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void atribuirEmChamadoFechadoRetorna409() throws Exception {
        when(service.atribuir(eq(1L), eq(2L)))
            .thenThrow(new ChamadoFechadoException("Chamado 1 está fechado e não pode receber técnico"));

        mvc.perform(patch("/chamados/1/atribuir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":2}"))
            .andExpect(status().isConflict());
    }

    @Test
    void statusSemValorRetorna400() throws Exception {
        mvc.perform(patch("/chamados/1/status").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}

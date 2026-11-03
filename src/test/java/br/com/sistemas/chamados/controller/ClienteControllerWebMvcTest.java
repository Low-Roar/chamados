package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(ClienteController.class)
class ClienteControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ClienteService service;

    @MockBean
    private ChamadoService chamadoService;

    private static final String VALIDO =
        "{\"nome\":\"Ana Souza\",\"email\":\"ana@exemplo.com\",\"telefone\":\"47999999999\"}";

    @Test
    void criarRetorna201() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Souza", "ana@exemplo.com", "47999999999"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void criarComDadosInvalidosRetorna400() throws Exception {
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"email-invalido\",\"telefone\":\"123\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes").isNotEmpty());
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe cliente com o e-mail ana@exemplo.com"));

        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isConflict());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(eq(99999L)))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 99999 não encontrado"));

        mvc.perform(get("/clientes/99999")).andExpect(status().isNotFound());
    }
}

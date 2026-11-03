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

import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.TecnicoService;

@WebMvcTest(TecnicoController.class)
class TecnicoControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TecnicoService service;

    private static final String VALIDO =
        "{\"nome\":\"Carlos\",\"email\":\"carlos@empresa.com\",\"perfil\":\"TECNICO\"}";

    @Test
    void criarRetorna201() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
            .thenReturn(new TecnicoResponse(1L, "Carlos", "carlos@empresa.com", Perfil.TECNICO));

        mvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.perfil").value("TECNICO"));
    }

    @Test
    void criarComDadosInvalidosRetorna400() throws Exception {
        mvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"email\":\"invalido\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void perfilInexistenteRetorna400() throws Exception {
        mvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Carlos\",\"email\":\"carlos@empresa.com\",\"perfil\":\"CHEFE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void emailDuplicadoRetorna409() throws Exception {
        when(service.criar(any(TecnicoRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe técnico com o e-mail carlos@empresa.com"));

        mvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isConflict());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(eq(99999L)))
            .thenThrow(new RecursoNaoEncontradoException("Técnico 99999 não encontrado"));

        mvc.perform(get("/tecnicos/99999")).andExpect(status().isNotFound());
    }
}

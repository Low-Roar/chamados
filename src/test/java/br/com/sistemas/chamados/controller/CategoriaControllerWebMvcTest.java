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

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;

@WebMvcTest(CategoriaController.class)
class CategoriaControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoriaService service;

    private static final String VALIDO = "{\"nome\":\"Hardware\",\"descricao\":\"Problemas físicos\"}";

    @Test
    void criarRetorna201() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
            .thenReturn(new CategoriaResponse(1L, "Hardware", "Problemas físicos"));

        mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nome").value("Hardware"));
    }

    @Test
    void criarSemNomeRetorna400() throws Exception {
        mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"descricao\":\"x\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nomeDuplicadoRetorna409() throws Exception {
        when(service.criar(any(CategoriaRequest.class)))
            .thenThrow(new RegraNegocioException("Já existe categoria com o nome Hardware"));

        mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
            .andExpect(status().isConflict());
    }

    @Test
    void buscarInexistenteRetorna404() throws Exception {
        when(service.buscar(eq(99999L)))
            .thenThrow(new RecursoNaoEncontradoException("Categoria 99999 não encontrada"));

        mvc.perform(get("/categorias/99999")).andExpect(status().isNotFound());
    }
}

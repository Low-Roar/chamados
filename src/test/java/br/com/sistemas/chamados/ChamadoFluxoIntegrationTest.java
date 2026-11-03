package br.com.sistemas.chamados;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Teste de integração (H2 em memória) com Chamado, Categoria, Técnico e Comentário.
 * Cada teste faz rollback ao final, então os dados não vazam entre os testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChamadoFluxoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private long clienteId;
    private long categoriaId;
    private long tecnicoId;

    @BeforeEach
    void prepararDados() throws Exception {
        clienteId = criar("/clientes",
            "{\"nome\":\"Ana Souza\",\"email\":\"ana.fluxo@exemplo.com\",\"telefone\":\"47999999999\"}");
        categoriaId = criar("/categorias", "{\"nome\":\"Rede\",\"descricao\":\"Problemas de rede\"}");
        tecnicoId = criar("/tecnicos",
            "{\"nome\":\"Carlos\",\"email\":\"carlos.fluxo@empresa.com\",\"perfil\":\"TECNICO\"}");
    }

    // ---------- auxiliares ----------

    private long criar(String url, String json) throws Exception {
        MvcResult r = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andReturn();
        return JsonPath.<Number>read(r.getResponse().getContentAsString(), "$.id").longValue();
    }

    private String chamadoJson(String titulo, String prioridade) {
        return "{\"titulo\":\"" + titulo + "\",\"descricao\":\"Descrição do problema\","
            + "\"prioridade\":\"" + prioridade + "\",\"clienteId\":" + clienteId
            + ",\"categoriaId\":" + categoriaId + "}";
    }

    private long criarChamado(String titulo, String prioridade) throws Exception {
        return criar("/chamados", chamadoJson(titulo, prioridade));
    }

    private void alterarStatus(long id, String status, int esperado) throws Exception {
        mvc.perform(patch("/chamados/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"))
            .andExpect(status().is(esperado));
    }

    // ---------- Categoria ----------

    @Test
    void categoriaNaoPermiteNomeDuplicado() throws Exception {
        mvc.perform(post("/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"rede\",\"descricao\":\"duplicada\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void categoriaCrudCompleto() throws Exception {
        mvc.perform(get("/categorias/" + categoriaId)).andExpect(status().isOk());
        mvc.perform(put("/categorias/" + categoriaId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Infraestrutura\",\"descricao\":\"Atualizada\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Infraestrutura"));
        mvc.perform(delete("/categorias/" + categoriaId)).andExpect(status().isNoContent());
        mvc.perform(get("/categorias/" + categoriaId)).andExpect(status().isNotFound());
    }

    @Test
    void categoriaComChamadosNaoPodeSerExcluida() throws Exception {
        criarChamado("Rede lenta", "BAIXA");
        mvc.perform(delete("/categorias/" + categoriaId)).andExpect(status().isConflict());
    }

    // ---------- Técnico ----------

    @Test
    void tecnicoNaoPermiteEmailDuplicado() throws Exception {
        mvc.perform(post("/tecnicos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Outro\",\"email\":\"carlos.fluxo@empresa.com\",\"perfil\":\"ADMIN\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void tecnicoCrudCompleto() throws Exception {
        mvc.perform(get("/tecnicos/" + tecnicoId)).andExpect(status().isOk());
        mvc.perform(put("/tecnicos/" + tecnicoId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Carlos Admin\",\"email\":\"carlos.fluxo@empresa.com\",\"perfil\":\"ADMIN\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value("ADMIN"));
        mvc.perform(delete("/tecnicos/" + tecnicoId)).andExpect(status().isNoContent());
        mvc.perform(get("/tecnicos/" + tecnicoId)).andExpect(status().isNotFound());
    }

    // ---------- Chamado: criação e regras ----------

    @Test
    void chamadoNasceAbertoComDataDeAbertura() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content(chamadoJson("Sem internet", "ALTA")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTO"))
            .andExpect(jsonPath("$.dataAbertura").isNotEmpty())
            .andExpect(jsonPath("$.dataFechamento").isEmpty())
            .andExpect(jsonPath("$.clienteNome").value("Ana Souza"))
            .andExpect(jsonPath("$.categoriaNome").value("Rede"));
    }

    @Test
    void chamadoComDadosInvalidosRetorna400() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"\",\"descricao\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void chamadoComClienteInexistenteRetorna404() throws Exception {
        mvc.perform(post("/chamados").contentType(MediaType.APPLICATION_JSON)
                .content(chamadoJson("Teste", "BAIXA").replace(
                    "\"clienteId\":" + clienteId, "\"clienteId\":99999")))
            .andExpect(status().isNotFound());
    }

    @Test
    void buscarChamadoInexistenteRetorna404() throws Exception {
        mvc.perform(get("/chamados/99999")).andExpect(status().isNotFound());
    }

    @Test
    void atribuirTecnico() throws Exception {
        long id = criarChamado("Impressora", "MEDIA");
        mvc.perform(patch("/chamados/" + id + "/atribuir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":" + tecnicoId + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tecnicoId").value(tecnicoId))
            .andExpect(jsonPath("$.tecnicoNome").value("Carlos"));
    }

    @Test
    void atribuirTecnicoInexistenteRetorna404() throws Exception {
        long id = criarChamado("Impressora", "MEDIA");
        mvc.perform(patch("/chamados/" + id + "/atribuir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":99999}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void transicoesValidasEInvalidasDeStatus() throws Exception {
        long id = criarChamado("Servidor fora", "CRITICA");

        alterarStatus(id, "AGUARDANDO_CLIENTE", 409);   // ABERTO -> AGUARDANDO_CLIENTE: inválida
        alterarStatus(id, "ABERTO", 409);               // mesmo status: inválida
        alterarStatus(id, "EM_ATENDIMENTO", 200);       // ABERTO -> EM_ATENDIMENTO: válida
        alterarStatus(id, "AGUARDANDO_CLIENTE", 200);   // EM_ATENDIMENTO -> AGUARDANDO_CLIENTE: válida
        alterarStatus(id, "EM_ATENDIMENTO", 200);       // AGUARDANDO_CLIENTE -> EM_ATENDIMENTO: válida
    }

    @Test
    void statusInexistenteRetorna400() throws Exception {
        long id = criarChamado("Servidor fora", "CRITICA");
        alterarStatus(id, "INEXISTENTE", 400);
    }

    @Test
    void fecharPreencheDataFechamentoEBloqueiaAlteracoes() throws Exception {
        long id = criarChamado("Notebook quebrado", "ALTA");

        mvc.perform(patch("/chamados/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FECHADO"))
            .andExpect(jsonPath("$.dataFechamento").isNotEmpty());

        // chamado fechado: não muda de status, não recebe técnico e não pode ser editado
        alterarStatus(id, "EM_ATENDIMENTO", 409);
        mvc.perform(patch("/chamados/" + id + "/atribuir").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":" + tecnicoId + "}"))
            .andExpect(status().isConflict());
        mvc.perform(put("/chamados/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(chamadoJson("Novo título", "BAIXA")))
            .andExpect(status().isConflict());
    }

    @Test
    void atualizarChamadoAberto() throws Exception {
        long id = criarChamado("Título antigo", "BAIXA");
        mvc.perform(put("/chamados/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(chamadoJson("Título novo", "ALTA")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titulo").value("Título novo"))
            .andExpect(jsonPath("$.prioridade").value("ALTA"))
            .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void excluirChamado() throws Exception {
        long id = criarChamado("Descartável", "BAIXA");
        mvc.perform(delete("/chamados/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/chamados/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void clienteComChamadosNaoPodeSerExcluido() throws Exception {
        criarChamado("Rede lenta", "BAIXA");
        mvc.perform(delete("/clientes/" + clienteId)).andExpect(status().isConflict());
    }

    // ---------- Comentários ----------

    @Test
    void comentariosAparecemNoDetalheDoChamado() throws Exception {
        long id = criarChamado("Rede lenta", "MEDIA");

        mvc.perform(post("/chamados/" + id + "/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Verificando o roteador\",\"autor\":\"Carlos\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.autor").value("Carlos"))
            .andExpect(jsonPath("$.data").isNotEmpty());

        mvc.perform(get("/chamados/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comentarios.length()").value(1))
            .andExpect(jsonPath("$.comentarios[0].texto").value("Verificando o roteador"));
    }

    @Test
    void comentarioInvalidoRetorna400() throws Exception {
        long id = criarChamado("Rede lenta", "MEDIA");
        mvc.perform(post("/chamados/" + id + "/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"\",\"autor\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void comentarEmChamadoInexistenteRetorna404() throws Exception {
        mvc.perform(post("/chamados/99999/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Oi\",\"autor\":\"Carlos\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void excluirChamadoComComentariosFunciona() throws Exception {
        long id = criarChamado("Com comentário", "BAIXA");
        mvc.perform(post("/chamados/" + id + "/comentarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Nota\",\"autor\":\"Carlos\"}"))
            .andExpect(status().isCreated());
        mvc.perform(delete("/chamados/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/chamados/" + id)).andExpect(status().isNotFound());
    }

    // ---------- Filtros e paginação ----------

    @Test
    void filtrosPorStatusPrioridadeECliente() throws Exception {
        criarChamado("Chamado 1", "ALTA");
        long segundo = criarChamado("Chamado 2", "BAIXA");
        alterarStatus(segundo, "EM_ATENDIMENTO", 200);

        mvc.perform(get("/chamados").param("clienteId", String.valueOf(clienteId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2));

        mvc.perform(get("/chamados").param("prioridade", "ALTA")
                .param("clienteId", String.valueOf(clienteId)))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.conteudo[0].titulo").value("Chamado 1"));

        mvc.perform(get("/chamados").param("status", "EM_ATENDIMENTO")
                .param("clienteId", String.valueOf(clienteId)))
            .andExpect(jsonPath("$.totalElementos").value(1))
            .andExpect(jsonPath("$.conteudo[0].titulo").value("Chamado 2"));

        mvc.perform(get("/chamados").param("status", "FECHADO")
                .param("prioridade", "ALTA")
                .param("clienteId", String.valueOf(clienteId)))
            .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void filtroComValorInvalidoRetorna400() throws Exception {
        mvc.perform(get("/chamados").param("status", "INEXISTENTE"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void paginacaoEOrdenacao() throws Exception {
        criarChamado("A primeiro", "BAIXA");
        criarChamado("B segundo", "BAIXA");
        criarChamado("C terceiro", "BAIXA");

        mvc.perform(get("/chamados").param("clienteId", String.valueOf(clienteId))
                .param("size", "2").param("page", "0").param("sort", "titulo,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo.length()").value(2))
            .andExpect(jsonPath("$.conteudo[0].titulo").value("A primeiro"))
            .andExpect(jsonPath("$.totalElementos").value(3))
            .andExpect(jsonPath("$.totalPaginas").value(2));

        mvc.perform(get("/chamados").param("clienteId", String.valueOf(clienteId))
                .param("size", "2").param("page", "1").param("sort", "titulo,asc"))
            .andExpect(jsonPath("$.conteudo.length()").value(1))
            .andExpect(jsonPath("$.conteudo[0].titulo").value("C terceiro"));
    }

    @Test
    void chamadosDeUmCliente() throws Exception {
        criarChamado("Chamado 1", "ALTA");
        criarChamado("Chamado 2", "BAIXA");

        mvc.perform(get("/clientes/" + clienteId + "/chamados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElementos").value(2));

        mvc.perform(get("/clientes/99999/chamados")).andExpect(status().isNotFound());
    }
}

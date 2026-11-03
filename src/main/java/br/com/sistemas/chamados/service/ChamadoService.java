package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.PaginaResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.ChamadoFechadoException;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.TransicaoStatusInvalidaException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ComentarioRepository comentarioRepository;

    public ChamadoService(ChamadoRepository repository,
                          ClienteRepository clienteRepository,
                          CategoriaRepository categoriaRepository,
                          TecnicoRepository tecnicoRepository,
                          ComentarioRepository comentarioRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.comentarioRepository = comentarioRepository;
    }

    /** Regra: o chamado nasce ABERTO e a dataAbertura é preenchida automaticamente. */
    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Chamado chamado = new Chamado();
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(buscarCategoria(dto.categoriaId()));
        chamado.setTecnico(dto.tecnicoId() != null ? buscarTecnico(dto.tecnicoId()) : null);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        return ChamadoResponse.de(repository.save(chamado));
    }

    /** Filtros opcionais + paginação/ordenação (Pageable). */
    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listar(
        StatusChamado status, Prioridade prioridade, Long clienteId, Pageable pageable) {

        Specification<Chamado> filtros = (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (prioridade != null) {
                predicados.add(cb.equal(root.get("prioridade"), prioridade));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
        return PaginaResponse.de(repository.findAll(filtros, pageable).map(ChamadoResponse::de));
    }

    /** GET /clientes/{id}/chamados: 404 se o cliente não existir. */
    @Transactional(readOnly = true)
    public PaginaResponse<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        buscarCliente(clienteId);
        return listar(null, null, clienteId, pageable);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        Chamado chamado = buscarEntity(id);
        return ChamadoDetalheResponse.de(chamado,
            comentarioRepository.findByChamadoIdOrderByDataAsc(id));
    }

    /** Regra: chamado fechado não pode ser alterado. */
    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);
        exigirNaoFechado(chamado, "Chamado " + id + " está fechado e não pode ser alterado");
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(buscarCategoria(dto.categoriaId()));
        chamado.setTecnico(dto.tecnicoId() != null ? buscarTecnico(dto.tecnicoId()) : null);
        return ChamadoResponse.de(chamado);
    }

    /** PATCH /chamados/{id}/atribuir. Regra: não atribui técnico a chamado fechado. */
    @Transactional
    public ChamadoResponse atribuir(Long id, Long tecnicoId) {
        Chamado chamado = buscarEntity(id);
        exigirNaoFechado(chamado,
            "Chamado " + id + " está fechado e não pode receber técnico");
        chamado.setTecnico(buscarTecnico(tecnicoId));
        return ChamadoResponse.de(chamado);
    }

    /** PATCH /chamados/{id}/status. Ao fechar, preenche dataFechamento. */
    @Transactional
    public ChamadoResponse alterarStatus(Long id, StatusChamado novoStatus) {
        Chamado chamado = buscarEntity(id);
        exigirNaoFechado(chamado, "Chamado " + id + " está fechado e não pode ser alterado");
        if (!chamado.getStatus().podeIrPara(novoStatus)) {
            throw new TransicaoStatusInvalidaException(
                "Transição de status inválida: " + chamado.getStatus() + " -> " + novoStatus);
        }
        chamado.setStatus(novoStatus);
        if (novoStatus == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        Chamado chamado = buscarEntity(id);
        comentarioRepository.deleteAll(comentarioRepository.findByChamadoIdOrderByDataAsc(id));
        repository.delete(chamado);
    }

    // ---------- métodos auxiliares ----------

    private void exigirNaoFechado(Chamado chamado, String mensagem) {
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new ChamadoFechadoException(mensagem);
        }
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Chamado " + id + " não encontrado"));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente " + id + " não encontrado"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria " + id + " não encontrada"));
    }

    private Tecnico buscarTecnico(Long id) {
        return tecnicoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Técnico " + id + " não encontrado"));
    }
}

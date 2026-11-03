package br.com.sistemas.chamados.repository;

import br.com.sistemas.chamados.entity.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** JpaSpecificationExecutor permite montar filtros opcionais (status, prioridade, cliente). */
public interface ChamadoRepository
        extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {
    boolean existsByClienteId(Long clienteId);
    boolean existsByCategoriaId(Long categoriaId);
    boolean existsByTecnicoId(Long tecnicoId);
}

package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

@DataJpaTest
class ChamadoRepositoryTest {

    @Autowired
    private ChamadoRepository chamadoRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void persisteChamadoComRelacionamentos() {
        Cliente cliente = clienteRepository.save(new Cliente("Ana", "ana@exemplo.com", null));
        Categoria categoria = categoriaRepository.save(new Categoria("Rede", null));

        Chamado chamado = new Chamado();
        chamado.setTitulo("Sem internet");
        chamado.setDescricao("A rede caiu no setor financeiro");
        chamado.setPrioridade(Prioridade.ALTA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setCliente(cliente);
        chamado.setCategoria(categoria);
        Chamado salvo = chamadoRepository.save(chamado);

        assertThat(salvo.getId()).isNotNull();
        assertThat(chamadoRepository.existsByClienteId(cliente.getId())).isTrue();
        assertThat(chamadoRepository.existsByCategoriaId(categoria.getId())).isTrue();
        assertThat(chamadoRepository.existsByTecnicoId(999L)).isFalse();
    }
}

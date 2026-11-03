package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import br.com.sistemas.chamados.entity.Cliente;

@DataJpaTest
class ClienteRepositoryTest {

    @Autowired
    private ClienteRepository repository;

    @Test
    void existsByEmailDetectaDuplicidade() {
        repository.save(new Cliente("Ana", "ana@exemplo.com", "47999999999"));

        assertThat(repository.existsByEmail("ana@exemplo.com")).isTrue();
        assertThat(repository.existsByEmail("outro@exemplo.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotIgnoraOProprioRegistro() {
        Cliente ana = repository.save(new Cliente("Ana", "ana@exemplo.com", null));
        Cliente bia = repository.save(new Cliente("Bia", "bia@exemplo.com", null));

        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("ana@exemplo.com", bia.getId())).isTrue();
    }
}

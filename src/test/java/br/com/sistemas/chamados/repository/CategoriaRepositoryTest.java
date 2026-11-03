package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import br.com.sistemas.chamados.entity.Categoria;

@DataJpaTest
class CategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository repository;

    @Test
    void existsByNomeIgnoraMaiusculasEMinusculas() {
        repository.save(new Categoria("Hardware", "Problemas físicos"));

        assertThat(repository.existsByNomeIgnoreCase("hardware")).isTrue();
        assertThat(repository.existsByNomeIgnoreCase("Software")).isFalse();
    }

    @Test
    void existsByNomeAndIdNotIgnoraOProprioRegistro() {
        Categoria hw = repository.save(new Categoria("Hardware", null));
        Categoria sw = repository.save(new Categoria("Software", null));

        assertThat(repository.existsByNomeIgnoreCaseAndIdNot("hardware", hw.getId())).isFalse();
        assertThat(repository.existsByNomeIgnoreCaseAndIdNot("hardware", sw.getId())).isTrue();
    }
}

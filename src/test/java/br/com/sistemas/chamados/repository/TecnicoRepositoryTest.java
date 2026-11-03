package br.com.sistemas.chamados.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Tecnico;

@DataJpaTest
class TecnicoRepositoryTest {

    @Autowired
    private TecnicoRepository repository;

    @Test
    void existsByEmailDetectaDuplicidade() {
        repository.save(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO));

        assertThat(repository.existsByEmail("carlos@empresa.com")).isTrue();
        assertThat(repository.existsByEmail("outro@empresa.com")).isFalse();
    }

    @Test
    void existsByEmailAndIdNotIgnoraOProprioRegistro() {
        Tecnico carlos = repository.save(new Tecnico("Carlos", "carlos@empresa.com", Perfil.TECNICO));
        Tecnico dani = repository.save(new Tecnico("Dani", "dani@empresa.com", Perfil.ADMIN));

        assertThat(repository.existsByEmailAndIdNot("carlos@empresa.com", carlos.getId())).isFalse();
        assertThat(repository.existsByEmailAndIdNot("carlos@empresa.com", dani.getId())).isTrue();
    }
}

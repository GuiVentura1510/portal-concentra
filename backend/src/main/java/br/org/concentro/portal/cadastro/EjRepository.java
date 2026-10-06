package br.org.concentro.portal.cadastro;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Acesso à tabela "ej". O Spring gera a implementação desta interface.
public interface EjRepository extends JpaRepository<Ej, Long> {

    // O nome do método vira a consulta: todas as EJs, em ordem de nome.
    List<Ej> findAllByOrderByNomeAsc();
}

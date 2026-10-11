package br.org.concentro.portal.cadastro;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Acesso à tabela "congressista". As buscas são sempre pelo e-mail já em minúsculas.
public interface CongressistaRepository extends JpaRepository<Congressista, Long> {

    boolean existsByEmail(String email);

    Optional<Congressista> findByEmail(String email);
}

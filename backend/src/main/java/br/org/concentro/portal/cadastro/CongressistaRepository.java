package br.org.concentro.portal.cadastro;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "congressista". As buscas são sempre pelo e-mail já em minúsculas.
public interface CongressistaRepository extends JpaRepository<Congressista, Long> {

    boolean existsByEmail(String email);

    Optional<Congressista> findByEmail(String email);

    // Traz a EJ junto, na mesma consulta, para o perfil mostrar o nome dela.
    @Query("select c from Congressista c join fetch c.ej where c.id = :id")
    Optional<Congressista> buscarComEj(@Param("id") Long id);
}

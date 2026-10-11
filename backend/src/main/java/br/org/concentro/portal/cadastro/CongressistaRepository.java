package br.org.concentro.portal.cadastro;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    @Query("select c from Congressista c where c.tripulacao is null")
    List<Congressista> semTripulacao();

    @Query("select c from Congressista c where c.tripulacao is not null")
    List<Congressista> comTripulacao();

    // Membros de uma tripulação com a EJ junto, em ordem de nome.
    @Query("select c from Congressista c join fetch c.ej where c.tripulacao.id = :id order by c.nome")
    List<Congressista> membrosDa(@Param("id") Long id);

    // Busca do painel do staff por parte do nome, do e-mail ou da EJ (o termo já vem em minúsculas).
    @Query("select c from Congressista c join fetch c.ej left join fetch c.tripulacao "
        + "where lower(c.nome) like :termo or c.email like :termo or lower(c.ej.nome) like :termo "
        + "order by c.nome")
    List<Congressista> buscar(@Param("termo") String termo, Pageable pagina);

    // A pessoa aparece no histórico do jogo (enviou ou avaliou missão, lançou pontos)
    // ou é capitã? Nesse caso o cadastro não pode ser removido.
    @Query(nativeQuery = true, value = """
        select exists (select 1 from conclusao where enviada_por = :id or avaliada_por = :id)
            or exists (select 1 from lancamento where criado_por = :id)
            or exists (select 1 from tripulacao where capitao_id = :id)
        """)
    boolean temHistorico(@Param("id") Long id);

    // Todos, com EJ e tripulação junto, para o cruzamento com a lista de inscritos.
    @Query("select c from Congressista c join fetch c.ej left join fetch c.tripulacao order by c.nome")
    List<Congressista> todosComEj();
}

package br.org.concentro.portal.tripulacao;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "tripulacao".
public interface TripulacaoRepository extends JpaRepository<Tripulacao, Long> {

    // Lista todas travando as linhas até o fim da transação. Assim dois cadastros
    // simultâneos não escolhem a mesma vaga ao mesmo tempo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tripulacao t order by t.id")
    List<Tripulacao> listarComTrava();

    List<Tripulacao> findAllByOrderByNomeAsc();

    // Uma tripulação, travada até o fim da transação. Assim dois anexos ao mesmo
    // tempo não passam juntos do limite de fotos do Diário de Bordo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tripulacao t where t.id = :id")
    Optional<Tripulacao> buscarComTrava(@Param("id") Long id);
}

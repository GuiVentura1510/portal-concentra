package br.org.concentro.portal.missao;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "conclusao".
public interface ConclusaoRepository extends JpaRepository<Conclusao, Long> {

    // Conclusões de uma tripulação, da mais antiga para a mais nova.
    @Query("select c from Conclusao c where c.tripulacao.id = :tripulacaoId order by c.enviadaEm, c.id")
    List<Conclusao> daTripulacao(@Param("tripulacaoId") Long tripulacaoId);

    boolean existsByMissaoIdAndTripulacaoIdAndStatusIn(Long missaoId, Long tripulacaoId,
        Collection<StatusConclusao> status);

    // Já existe conclusão pendente ou aprovada desta tripulação nesta missão?
    default boolean existeAtiva(Long missaoId, Long tripulacaoId) {
        return existsByMissaoIdAndTripulacaoIdAndStatusIn(missaoId, tripulacaoId,
            List.of(StatusConclusao.PENDENTE, StatusConclusao.APROVADA));
    }

    // Fila do staff, na ordem de chegada, com missão, tripulação e autor junto.
    @Query("select c from Conclusao c join fetch c.missao join fetch c.tripulacao left join fetch c.enviadaPor "
        + "where c.status = :status order by c.enviadaEm, c.id")
    List<Conclusao> comStatus(@Param("status") StatusConclusao status);
}

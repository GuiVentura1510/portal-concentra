package br.org.concentro.portal.missao;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "conclusao".
public interface ConclusaoRepository extends JpaRepository<Conclusao, Long> {

    List<StatusConclusao> ATIVAS = List.of(StatusConclusao.PENDENTE, StatusConclusao.APROVADA);

    // Conclusões das missões da tripulação, da mais antiga para a mais nova.
    // As individuais dos membros ficam de fora (têm a pessoa preenchida).
    @Query("select c from Conclusao c where c.tripulacao.id = :tripulacaoId and c.congressista is null "
        + "order by c.enviadaEm, c.id")
    List<Conclusao> daTripulacao(@Param("tripulacaoId") Long tripulacaoId);

    // Conclusões das missões individuais de uma pessoa, da mais antiga para a mais nova.
    @Query("select c from Conclusao c where c.congressista.id = :congressistaId order by c.enviadaEm, c.id")
    List<Conclusao> doCongressista(@Param("congressistaId") Long congressistaId);

    boolean existsByMissaoIdAndTripulacaoIdAndCongressistaIsNullAndStatusIn(Long missaoId, Long tripulacaoId,
        Collection<StatusConclusao> status);

    boolean existsByMissaoIdAndCongressistaIdAndStatusIn(Long missaoId, Long congressistaId,
        Collection<StatusConclusao> status);

    boolean existsByMissaoId(Long missaoId);

    // Já existe conclusão pendente ou aprovada desta tripulação nesta missão?
    default boolean existeAtiva(Long missaoId, Long tripulacaoId) {
        return existsByMissaoIdAndTripulacaoIdAndCongressistaIsNullAndStatusIn(missaoId, tripulacaoId, ATIVAS);
    }

    // Já existe conclusão pendente ou aprovada desta pessoa nesta missão individual?
    default boolean existeAtivaIndividual(Long missaoId, Long congressistaId) {
        return existsByMissaoIdAndCongressistaIdAndStatusIn(missaoId, congressistaId, ATIVAS);
    }

    // Fila do staff, na ordem de chegada, com missão, tripulação, pessoa e autor junto.
    @Query("select c from Conclusao c join fetch c.missao left join fetch c.tripulacao "
        + "left join fetch c.congressista left join fetch c.enviadaPor "
        + "where c.status = :status order by c.enviadaEm, c.id")
    List<Conclusao> comStatus(@Param("status") StatusConclusao status);
}

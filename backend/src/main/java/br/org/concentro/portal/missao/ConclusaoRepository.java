package br.org.concentro.portal.missao;

import br.org.concentro.portal.cadastro.Congressista;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    boolean existsByMissaoIdAndStatusIn(Long missaoId, Collection<StatusConclusao> status);

    // Apaga os envios retirados de uma missão, antes de excluí-la.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Conclusao c where c.missao.id = :missaoId "
        + "and c.status = br.org.concentro.portal.missao.StatusConclusao.RECUSADA")
    int apagarRetiradasDaMissao(@Param("missaoId") Long missaoId);

    // Já existe conclusão pendente ou aprovada desta tripulação nesta missão?
    default boolean existeAtiva(Long missaoId, Long tripulacaoId) {
        return existsByMissaoIdAndTripulacaoIdAndCongressistaIsNullAndStatusIn(missaoId, tripulacaoId, ATIVAS);
    }

    // Já existe conclusão pendente ou aprovada desta pessoa nesta missão individual?
    default boolean existeAtivaIndividual(Long missaoId, Long congressistaId) {
        return existsByMissaoIdAndCongressistaIdAndStatusIn(missaoId, congressistaId, ATIVAS);
    }

    // Fila do staff, na ordem de chegada, com missão, tripulação, pessoa e autor junto.
    // Aba Envios do staff: conclusões que valem pontos (envios aceitos e registros do
    // staff), da mais nova para a mais antiga. A página limita a quantidade.
    @Query("select c from Conclusao c join fetch c.missao left join fetch c.tripulacao "
        + "left join fetch c.congressista left join fetch c.enviadaPor "
        + "where c.status = br.org.concentro.portal.missao.StatusConclusao.APROVADA "
        + "order by c.enviadaEm desc, c.id desc")
    List<Conclusao> aceitasRecentes(Pageable pagina);

    // Retira uma conclusão aceita. Só muda a linha se ela ainda estiver aprovada:
    // devolve 1 se retirou e 0 se outro staff já tinha retirado.
    // Limpa o contexto do JPA depois, porque a entidade em memória fica desatualizada.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Conclusao c set c.status = br.org.concentro.portal.missao.StatusConclusao.RECUSADA, "
        + "c.avaliadaPor = :staff, c.avaliadaEm = :agora, c.motivoRecusa = :motivo "
        + "where c.id = :id and c.status = br.org.concentro.portal.missao.StatusConclusao.APROVADA")
    int retirar(@Param("id") Long id, @Param("staff") Congressista staff, @Param("agora") Instant agora,
        @Param("motivo") String motivo);
}

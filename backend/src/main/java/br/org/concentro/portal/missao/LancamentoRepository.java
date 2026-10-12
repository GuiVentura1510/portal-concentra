package br.org.concentro.portal.missao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "lancamento".
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    boolean existsByMissaoId(Long missaoId);

    // Apaga os pontos de uma conclusão retirada pelo staff (no máximo um lançamento,
    // pela regra "conclusao_id unique" da V6).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Lancamento l where l.conclusao.id = :conclusaoId")
    int apagarDaConclusao(@Param("conclusaoId") Long conclusaoId);

    // Ranking das tripulações: só os pontos das missões da tripulação. Os das missões
    // individuais (com a pessoa preenchida) contam só no ranking individual.
    // Cada linha: id, nome, pontos, missões.
    @Query(nativeQuery = true, value = """
        select t.id, t.nome, coalesce(sum(l.pontos), 0) as pontos, count(distinct l.missao_id) as missoes
        from tripulacao t
        left join lancamento l on l.tripulacao_id = t.id and l.congressista_id is null
        group by t.id, t.nome
        order by pontos desc, t.nome asc
        """)
    List<Object[]> placar();

    // Ranking individual: só os pontos das missões individuais, de quem já pontuou.
    // Cada linha: id, nome, EJ, tripulação (pode ser nula), pontos, missões e a
    // chave da foto (pode ser nula).
    @Query(nativeQuery = true, value = """
        select c.id, c.nome, e.nome as ej, t.nome as tripulacao, sum(l.pontos) as pontos,
               count(distinct l.missao_id) as missoes, c.foto_chave
        from lancamento l
        join congressista c on c.id = l.congressista_id
        join ej e on e.id = c.ej_id
        left join tripulacao t on t.id = c.tripulacao_id
        group by c.id, c.nome, e.nome, t.nome, c.foto_chave
        order by pontos desc, c.nome asc
        """)
    List<Object[]> placarIndividual();
}

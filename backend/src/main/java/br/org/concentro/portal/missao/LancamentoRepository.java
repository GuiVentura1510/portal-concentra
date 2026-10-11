package br.org.concentro.portal.missao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "lancamento".
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    boolean existsByMissaoId(Long missaoId);

    // Total de pontos e de missões da tripulação cumpridas por cada tripulação, já em
    // ordem de ranking. Com "individuais" verdadeiro, os pontos das missões individuais
    // dos membros também somam (configuração app.ranking.individuais-na-tripulacao).
    // Cada linha: id, nome, pontos, missões.
    @Query(nativeQuery = true, value = """
        select t.id, t.nome, coalesce(sum(l.pontos), 0) as pontos,
               count(distinct l.missao_id) filter (where l.congressista_id is null) as missoes
        from tripulacao t
        left join lancamento l on l.tripulacao_id = t.id and (:individuais or l.congressista_id is null)
        group by t.id, t.nome
        order by pontos desc, t.nome asc
        """)
    List<Object[]> placar(@Param("individuais") boolean individuais);
}

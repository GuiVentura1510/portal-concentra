package br.org.concentro.portal.missao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Acesso à tabela "lancamento".
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    boolean existsByMissaoId(Long missaoId);

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
    // Cada linha: id, nome, EJ, tripulação (pode ser nula), pontos, missões.
    @Query(nativeQuery = true, value = """
        select c.id, c.nome, e.nome as ej, t.nome as tripulacao, sum(l.pontos) as pontos,
               count(distinct l.missao_id) as missoes
        from lancamento l
        join congressista c on c.id = l.congressista_id
        join ej e on e.id = c.ej_id
        left join tripulacao t on t.id = c.tripulacao_id
        group by c.id, c.nome, e.nome, t.nome
        order by pontos desc, c.nome asc
        """)
    List<Object[]> placarIndividual();
}

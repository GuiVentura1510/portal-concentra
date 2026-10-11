package br.org.concentro.portal.missao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Acesso à tabela "lancamento".
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    // Total de pontos e de missões cumpridas de cada tripulação, já em ordem de ranking.
    // Cada linha: id, nome, pontos, missões.
    @Query(nativeQuery = true, value = """
        select t.id, t.nome, coalesce(sum(l.pontos), 0) as pontos, count(distinct l.missao_id) as missoes
        from tripulacao t
        left join lancamento l on l.tripulacao_id = t.id
        group by t.id, t.nome
        order by pontos desc, t.nome asc
        """)
    List<Object[]> placar();
}

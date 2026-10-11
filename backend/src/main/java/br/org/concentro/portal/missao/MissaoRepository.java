package br.org.concentro.portal.missao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "missao".
public interface MissaoRepository extends JpaRepository<Missao, Long> {

    // Em ordem de horário; missões sem horário vão para o fim.
    @Query("select m from Missao m order by m.inicio asc nulls last, m.id asc")
    List<Missao> todasEmOrdem();

    @Query("select m from Missao m where m.visivel = true order by m.inicio asc nulls last, m.id asc")
    List<Missao> visiveisEmOrdem();

    // Ocupa uma vaga numa única instrução: soma 1 só se ainda houver vaga.
    // Devolve 1 se conseguiu e 0 se as vagas acabaram. Por ser um "update ... where",
    // dois staffs aprovando ao mesmo tempo nunca passam do limite.
    // Limpa o contexto do JPA depois, porque a entidade em memória fica desatualizada.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Missao m set m.vagasUsadas = m.vagasUsadas + 1 "
        + "where m.id = :id and (m.vagas is null or m.vagasUsadas < m.vagas)")
    int ocuparVaga(@Param("id") Long id);
}

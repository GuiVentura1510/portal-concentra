package br.org.concentro.portal.missao;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Acesso à tabela "missao".
public interface MissaoRepository extends JpaRepository<Missao, Long> {

    // Em ordem de horário; missões sem horário vão para o fim.
    @Query("select m from Missao m order by m.inicio asc nulls last, m.id asc")
    List<Missao> todasEmOrdem();
}

package br.org.concentro.portal.tripulacao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acesso à tabela "foto_diario". As listas montam a resposta direto na consulta,
// sem carregar os bytes das imagens.
public interface FotoDiarioRepository extends JpaRepository<FotoDiario, Long> {

    // Diário de uma tripulação, na ordem em que as fotos foram anexadas.
    @Query("select new br.org.concentro.portal.tripulacao.FotoDiarioResposta("
        + "f.id, f.chave, a.id, a.nome, t.nome, f.legenda, f.criadaEm) "
        + "from FotoDiario f join f.autor a join f.tripulacao t "
        + "where t.id = :tripulacaoId order by f.criadaEm, f.id")
    List<FotoDiarioResposta> daTripulacao(@Param("tripulacaoId") Long tripulacaoId);

    // Aba Diário do staff: as mais novas primeiro.
    @Query("select new br.org.concentro.portal.tripulacao.FotoDiarioResposta("
        + "f.id, f.chave, a.id, a.nome, t.nome, f.legenda, f.criadaEm) "
        + "from FotoDiario f join f.autor a join f.tripulacao t order by f.criadaEm desc, f.id desc")
    List<FotoDiarioResposta> recentes(Pageable pagina);

    @Query("select count(f) from FotoDiario f where f.tripulacao.id = :tripulacaoId")
    long contarDaTripulacao(@Param("tripulacaoId") Long tripulacaoId);

    @Query("select f.conteudo from FotoDiario f where f.chave = :chave")
    Optional<byte[]> conteudoPorChave(@Param("chave") String chave);
}

package br.org.concentro.portal.missao;

import java.time.Instant;

// Uma missão como a API mostra. Os horários saem em UTC (ex.: 2026-10-17T13:30:00Z).
public record MissaoResposta(
    Long id,
    String titulo,
    String descricao,
    int pontos,
    Integer vagas,
    Instant inicio,
    Instant fim,
    FormaConclusao conclusao,
    boolean visivel,
    int vagasUsadas) {

    static MissaoResposta de(Missao m) {
        return new MissaoResposta(m.getId(), m.getTitulo(), m.getDescricao(), m.getPontos(), m.getVagas(),
            m.getInicio(), m.getFim(), m.getConclusao(), m.isVisivel(), m.getVagasUsadas());
    }
}

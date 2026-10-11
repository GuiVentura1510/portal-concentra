package br.org.concentro.portal.missao;

import java.time.Instant;

// Uma conclusão como o staff vê na fila.
public record ConclusaoResposta(
    Long id,
    Long missaoId,
    String missaoTitulo,
    Long tripulacaoId,
    String tripulacaoNome,
    StatusConclusao status,
    String resposta,
    String enviadaPor,
    Instant enviadaEm,
    String motivoRecusa) {

    static ConclusaoResposta de(Conclusao c) {
        String autor = c.getEnviadaPor() == null ? null : c.getEnviadaPor().getNome();
        return new ConclusaoResposta(c.getId(), c.getMissao().getId(), c.getMissao().getTitulo(),
            c.getTripulacao().getId(), c.getTripulacao().getNome(), c.getStatus(), c.getResposta(), autor,
            c.getEnviadaEm(), c.getMotivoRecusa());
    }
}

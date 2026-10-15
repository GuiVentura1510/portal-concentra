package br.org.concentro.portal.missao;

import java.time.Instant;
import java.util.List;

// Uma conclusão como o staff vê na fila. Nas missões individuais vem a pessoa;
// a tripulação pode vir vazia se a pessoa ainda não tiver uma.
public record ConclusaoResposta(
    Long id,
    Long missaoId,
    String missaoTitulo,
    AlcanceMissao alcance,
    Long tripulacaoId,
    String tripulacaoNome,
    Long congressistaId,
    String congressistaNome,
    StatusConclusao status,
    String resposta,
    String enviadaPor,
    Instant enviadaEm,
    String motivoRecusa,
    // A primeira foto (para o site antigo) e todas as fotos do envio (V11).
    String fotoChave,
    List<String> fotoChaves) {

    static ConclusaoResposta de(Conclusao c) {
        String autor = c.getEnviadaPor() == null ? null : c.getEnviadaPor().getNome();
        Long tripulacaoId = c.getTripulacao() == null ? null : c.getTripulacao().getId();
        String tripulacaoNome = c.getTripulacao() == null ? null : c.getTripulacao().getNome();
        Long pessoaId = c.getCongressista() == null ? null : c.getCongressista().getId();
        String pessoaNome = c.getCongressista() == null ? null : c.getCongressista().getNome();
        return new ConclusaoResposta(c.getId(), c.getMissao().getId(), c.getMissao().getTitulo(),
            c.getMissao().getAlcance(), tripulacaoId, tripulacaoNome, pessoaId, pessoaNome, c.getStatus(),
            c.getResposta(), autor, c.getEnviadaEm(), c.getMotivoRecusa(), c.getFotoChave(),
            c.getFotoChaves());
    }
}

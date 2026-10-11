package br.org.concentro.portal.missao;

import java.time.Instant;

// Uma missão como a tripulação vê: dados, situação, o que a tripulação já
// enviou e se quem está logado pode enviar agora.
public record MissaoDaTripulacao(
    Long id,
    String titulo,
    String descricao,
    int pontos,
    Integer vagas,
    // Vazio quando a missão não tem limite de vagas.
    Integer vagasRestantes,
    Instant inicio,
    Instant fim,
    FormaConclusao conclusao,
    SituacaoMissao situacao,
    MinhaConclusao minhaConclusao,
    boolean possoEnviar) {

    // A última tentativa da tripulação nesta missão.
    public record MinhaConclusao(StatusConclusao status, String resposta, String motivoRecusa) {
    }
}

package br.org.concentro.portal.missao;

import java.time.Instant;

// Uma missão como o congressista vê: dados, situação, o que já foi enviado e se
// quem está logado pode enviar agora. Nas missões da tripulação, "minhaConclusao"
// é a da tripulação; nas individuais, a da própria pessoa.
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
    AlcanceMissao alcance,
    Comprovacao comprovacao,
    SituacaoMissao situacao,
    MinhaConclusao minhaConclusao,
    boolean possoEnviar) {

    // A última tentativa nesta missão.
    // "fotoChave" forma o endereço /api/comprovacoes/{chave}; vazia sem foto.
    public record MinhaConclusao(StatusConclusao status, String resposta, String motivoRecusa, String fotoChave) {
    }
}

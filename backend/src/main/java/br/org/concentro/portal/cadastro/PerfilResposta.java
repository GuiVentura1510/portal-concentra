package br.org.concentro.portal.cadastro;

// Dados da pessoa logada, para as telas de perfil e home.
public record PerfilResposta(
    Long id,
    String nome,
    String email,
    Long ejId,
    String ejNome,
    Cargo cargo,
    TempoMej tempoMej,
    AreaAtuacao areaAtuacao,
    boolean staff) {
}

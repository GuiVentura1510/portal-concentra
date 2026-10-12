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
    boolean staff,
    // Chave da foto de perfil; nula para quem não enviou foto.
    String fotoChave,
    // Falso depois da montagem das tripulações: a tela trava EJ e tempo de MEJ.
    boolean podeMudarEjETempo) {
}

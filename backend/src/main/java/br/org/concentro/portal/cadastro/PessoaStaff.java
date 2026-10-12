package br.org.concentro.portal.cadastro;

// Um cadastro como o staff vê na aba Pessoas.
public record PessoaStaff(
    Long id,
    String nome,
    String email,
    String ejNome,
    Cargo cargo,
    String tripulacaoNome,
    boolean staff,
    String fotoChave) {
}

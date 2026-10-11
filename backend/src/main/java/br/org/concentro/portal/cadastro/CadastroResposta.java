package br.org.concentro.portal.cadastro;

// O que a API devolve depois de um cadastro. Nunca inclui a senha nem o hash.
public record CadastroResposta(Long id, String nome, String email, Long ejId) {
}

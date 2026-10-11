package br.org.concentro.portal.acesso;

// O que a API devolve no login: o token e o nome, para o site saudar a pessoa.
public record LoginResposta(String token, String nome) {
}

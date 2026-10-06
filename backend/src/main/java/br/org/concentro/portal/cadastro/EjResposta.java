package br.org.concentro.portal.cadastro;

// Formato de uma EJ na resposta da API. Fica separado da classe Ej para
// a tabela poder ganhar colunas sem mudar o que o front recebe.
public record EjResposta(Long id, String nome) {
}

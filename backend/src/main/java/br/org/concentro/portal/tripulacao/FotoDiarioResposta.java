package br.org.concentro.portal.tripulacao;

import java.time.Instant;

// Uma foto do Diário de Bordo como a API mostra. "chave" forma o endereço
// /api/diario/{chave}; "autorId" diz ao site quais fotos a pessoa pode apagar;
// "legenda" é vazia quando quem anexou não escreveu nada.
public record FotoDiarioResposta(
    Long id,
    String chave,
    Long autorId,
    String autorNome,
    String tripulacaoNome,
    String legenda,
    Instant criadaEm) {
}

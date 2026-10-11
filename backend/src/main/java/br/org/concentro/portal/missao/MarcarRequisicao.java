package br.org.concentro.portal.missao;

// Quem o staff está marcando como tendo cumprido a missão: a tripulação, nas
// missões da tripulação, ou a pessoa, nas individuais. O serviço confere qual vale.
public record MarcarRequisicao(Long tripulacaoId, Long congressistaId) {
}

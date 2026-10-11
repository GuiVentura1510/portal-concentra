package br.org.concentro.portal.missao;

import jakarta.validation.constraints.NotNull;

// Tripulação que o staff está marcando como tendo cumprido a missão.
public record MarcarRequisicao(@NotNull Long tripulacaoId) {
}

package br.org.concentro.portal.tripulacao;

import jakarta.validation.constraints.NotNull;

// Quem a tripulação escolheu como capitão.
public record CapitaoRequisicao(@NotNull Long congressistaId) {
}

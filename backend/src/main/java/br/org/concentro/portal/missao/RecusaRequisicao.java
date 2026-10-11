package br.org.concentro.portal.missao;

import jakarta.validation.constraints.Size;

// Motivo da recusa, mostrado para a tripulação. Pode ficar vazio.
public record RecusaRequisicao(@Size(max = 500) String motivo) {
}

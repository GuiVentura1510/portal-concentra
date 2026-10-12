package br.org.concentro.portal.missao;

import jakarta.validation.constraints.Size;

// Motivo da retirada de um envio, mostrado para a tripulação ou a pessoa.
// Pode ficar vazio.
public record RecusaRequisicao(@Size(max = 500) String motivo) {
}

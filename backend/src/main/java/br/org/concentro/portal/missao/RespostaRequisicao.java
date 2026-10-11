package br.org.concentro.portal.missao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Resposta de uma missão enviada pelo capitão.
public record RespostaRequisicao(@NotBlank @Size(max = 4000) String texto) {
}

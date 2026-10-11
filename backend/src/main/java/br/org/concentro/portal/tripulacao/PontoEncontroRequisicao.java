package br.org.concentro.portal.tripulacao;

import jakarta.validation.constraints.Size;

// Onde a tripulação se encontra. Texto vazio apaga o ponto de encontro.
public record PontoEncontroRequisicao(@Size(max = 200) String texto) {
}

package br.org.concentro.portal.missao;

import jakarta.validation.constraints.Size;

// Resposta de uma missão: texto, foto (JPEG em base64, já reduzido pelo site) ou
// os dois. O que é obrigatório depende da comprovação da missão (ConclusaoService).
// O limite do base64 cobre os 400 KB do ConclusaoService.LIMITE_FOTO_BYTES.
public record RespostaRequisicao(
    @Size(max = 4000) String texto,
    @Size(max = 560_000) String foto) {
}

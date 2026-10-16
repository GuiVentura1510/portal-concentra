package br.org.concentro.portal.tripulacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Foto anexada ao Diário de Bordo: JPEG em base64, já reduzido pelo site, e a
// legenda escrita por quem anexou (opcional; 30 caracteres cabem na polaroide).
// O limite do base64 cobre os 400 KB do DiarioService.LIMITE_FOTO_BYTES.
public record FotoDiarioRequisicao(
    @NotBlank @Size(max = 560_000) String imagem,
    @Size(max = 30) String legenda) {
}

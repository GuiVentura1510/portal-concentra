package br.org.concentro.portal.cadastro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Foto enviada pelo site: um JPEG em base64. O tamanho máximo do texto tem folga
// sobre o limite em bytes conferido no FotoService (base64 ocupa 4/3 do arquivo).
public record FotoRequisicao(@NotBlank @Size(max = 140_000) String imagem) {
}

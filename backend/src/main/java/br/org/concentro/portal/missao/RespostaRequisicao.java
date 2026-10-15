package br.org.concentro.portal.missao;

import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

// Resposta de uma missão: texto, fotos (JPEG em base64, já reduzidas pelo site) ou
// os dois. O que é obrigatório depende da comprovação da missão (ConclusaoService).
// O limite do base64 cobre os 400 KB do ConclusaoService.LIMITE_FOTO_BYTES.
// "foto" é o campo de antes da V11 (uma foto só); "fotos" aceita até 5.
public record RespostaRequisicao(
    @Size(max = 4000) String texto,
    @Size(max = 560_000) String foto,
    @Size(max = Missao.MAX_FOTOS) List<@Size(max = 560_000) String> fotos) {

    // Todas as fotos mandadas, juntando os dois campos e ignorando as vazias.
    public List<String> todasAsFotos() {
        List<String> todas = new ArrayList<>();
        if (foto != null && !foto.isBlank()) {
            todas.add(foto);
        }
        if (fotos != null) {
            fotos.stream().filter(f -> f != null && !f.isBlank()).forEach(todas::add);
        }
        return todas;
    }
}

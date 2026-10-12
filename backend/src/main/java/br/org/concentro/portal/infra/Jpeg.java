package br.org.concentro.portal.infra;

import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Confere uma imagem enviada pelo site em base64: texto válido, dentro do limite
// e com cara de JPEG. Usado pela foto de perfil e pela foto dos envios de missão.
public final class Jpeg {

    private Jpeg() {
    }

    public static byte[] decodificar(String base64, int limiteBytes, String grandeDemais) {
        byte[] conteudo;
        try {
            conteudo = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem inválida.");
        }
        if (conteudo.length > limiteBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, grandeDemais);
        }
        // Todo arquivo JPEG começa com os bytes FF D8 FF.
        boolean jpeg = conteudo.length > 3
            && (conteudo[0] & 0xFF) == 0xFF
            && (conteudo[1] & 0xFF) == 0xD8
            && (conteudo[2] & 0xFF) == 0xFF;
        if (!jpeg) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto precisa ser uma imagem JPEG.");
        }
        return conteudo;
    }
}

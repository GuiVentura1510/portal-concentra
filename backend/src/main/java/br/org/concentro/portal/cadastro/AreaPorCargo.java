package br.org.concentro.portal.cadastro;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Regra da área de atuação, usada no cadastro e na edição do perfil:
// trainee fica sem área (o que vier é descartado); os outros cargos precisam de uma.
final class AreaPorCargo {

    private AreaPorCargo() {
    }

    static AreaAtuacao conferir(Cargo cargo, AreaAtuacao area) {
        if (!cargo.temAreaDeAtuacao()) {
            return null;
        }
        if (area == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha a área de atuação.");
        }
        return area;
    }
}

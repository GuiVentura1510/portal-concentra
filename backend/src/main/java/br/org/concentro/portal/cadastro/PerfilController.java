package br.org.concentro.portal.cadastro;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Devolve os dados de quem está logado. Exige o token: o id vem dele,
// então ninguém consegue ver o perfil de outra pessoa.
@RestController
public class PerfilController {

    private final CongressistaRepository congressistas;

    public PerfilController(CongressistaRepository congressistas) {
        this.congressistas = congressistas;
    }

    @GetMapping("/api/perfil")
    public PerfilResposta perfil(@AuthenticationPrincipal Jwt token) {
        Long id = Long.valueOf(token.getSubject());
        Congressista c = congressistas.buscarComEj(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
        return new PerfilResposta(c.getId(), c.getNome(), c.getEmail(), c.getEj().getId(),
            c.getEj().getNome(), c.getCargo(), c.getTempoMej(), c.getAreaAtuacao());
    }
}

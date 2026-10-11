package br.org.concentro.portal.tripulacao;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Tripulação de quem está logado. Responde 404 até a montagem, em 15/10.
@RestController
public class TripulacaoController {

    private final TripulacaoService tripulacoes;

    public TripulacaoController(TripulacaoService tripulacoes) {
        this.tripulacoes = tripulacoes;
    }

    @GetMapping("/api/tripulacao")
    public TripulacaoResposta minha(@AuthenticationPrincipal Jwt token) {
        return tripulacoes.daPessoa(Long.valueOf(token.getSubject()));
    }
}

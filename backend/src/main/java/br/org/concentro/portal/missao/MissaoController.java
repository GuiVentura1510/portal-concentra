package br.org.concentro.portal.missao;

import br.org.concentro.portal.missao.MissaoDaTripulacao.MinhaConclusao;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Missões e ranking para quem está logado.
@RestController
public class MissaoController {

    private final ConclusaoService conclusoes;
    private final RankingService ranking;

    public MissaoController(ConclusaoService conclusoes, RankingService ranking) {
        this.conclusoes = conclusoes;
        this.ranking = ranking;
    }

    @GetMapping("/api/missoes")
    public List<MissaoDaTripulacao> missoes(@AuthenticationPrincipal Jwt token) {
        return conclusoes.paraCongressista(Long.valueOf(token.getSubject()));
    }

    @PostMapping("/api/missoes/{id}/resposta")
    @ResponseStatus(HttpStatus.CREATED)
    public MinhaConclusao responder(@PathVariable Long id, @AuthenticationPrincipal Jwt token,
            @Valid @RequestBody RespostaRequisicao requisicao) {
        return conclusoes.enviar(id, Long.valueOf(token.getSubject()), requisicao.texto());
    }

    @GetMapping("/api/ranking")
    public List<PosicaoRanking> ranking() {
        return ranking.ranking();
    }
}

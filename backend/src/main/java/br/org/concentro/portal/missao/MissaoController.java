package br.org.concentro.portal.missao;

import br.org.concentro.portal.missao.MissaoDaTripulacao.MinhaConclusao;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
        return conclusoes.enviar(id, Long.valueOf(token.getSubject()), requisicao.texto(), requisicao.foto());
    }

    // Foto de um envio. Pública como a foto de perfil (a tag <img> não envia o
    // token): a chave sorteada só chega ao staff e a quem enviou. Foto nova = chave
    // nova, então o navegador pode guardar a imagem.
    @GetMapping("/api/comprovacoes/{chave}")
    public ResponseEntity<byte[]> foto(@PathVariable String chave) {
        byte[] conteudo = conclusoes.lerFoto(chave)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada."));
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
            .body(conteudo);
    }

    @GetMapping("/api/ranking")
    public List<PosicaoRanking> ranking() {
        return ranking.ranking();
    }

    // Ranking individual: só as missões individuais, com a posição de quem pediu.
    @GetMapping("/api/ranking/individual")
    public RankingIndividual rankingIndividual(@AuthenticationPrincipal Jwt token) {
        return ranking.individual(Long.valueOf(token.getSubject()));
    }
}

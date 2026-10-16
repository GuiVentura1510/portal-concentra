package br.org.concentro.portal.tripulacao;

import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Diário de Bordo da tripulação de quem está logado (aba Tripulação).
@RestController
public class DiarioController {

    private final DiarioService diario;

    public DiarioController(DiarioService diario) {
        this.diario = diario;
    }

    @GetMapping("/api/tripulacao/diario")
    public DiarioResposta listar(@AuthenticationPrincipal Jwt token) {
        return diario.listar(Long.valueOf(token.getSubject()));
    }

    @PostMapping("/api/tripulacao/diario")
    @ResponseStatus(HttpStatus.CREATED)
    public FotoDiarioResposta anexar(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody FotoDiarioRequisicao requisicao) {
        return diario.anexar(Long.valueOf(token.getSubject()), requisicao.imagem(), requisicao.legenda());
    }

    @DeleteMapping("/api/tripulacao/diario/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        diario.apagar(id, Long.valueOf(token.getSubject()));
    }

    // Foto do diário. Pública como as outras fotos (a tag <img> não envia o token):
    // a chave sorteada só chega à tripulação e ao staff. Foto nova = chave nova, então
    // o navegador pode guardar a imagem.
    @GetMapping("/api/diario/{chave}")
    public ResponseEntity<byte[]> foto(@PathVariable String chave) {
        byte[] conteudo = diario.lerFoto(chave)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada."));
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
            .body(conteudo);
    }
}

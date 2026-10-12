package br.org.concentro.portal.cadastro;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Foto de perfil. O envio e a remoção exigem login e valem só para a própria
// pessoa (o id vem do token). A leitura é pública: a tag <img> do navegador não
// envia o token, e quem protege a foto é a chave sorteada no endereço.
@RestController
public class FotoController {

    private final FotoService fotos;

    public FotoController(FotoService fotos) {
        this.fotos = fotos;
    }

    @PutMapping("/api/perfil/foto")
    public FotoResposta enviar(@AuthenticationPrincipal Jwt token, @Valid @RequestBody FotoRequisicao requisicao) {
        return fotos.enviar(Long.valueOf(token.getSubject()), requisicao.imagem());
    }

    @DeleteMapping("/api/perfil/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal Jwt token) {
        fotos.remover(Long.valueOf(token.getSubject()));
    }

    // O navegador pode guardar a imagem por um ano sem perguntar de novo: uma foto
    // nova ganha outra chave, então este endereço nunca muda de conteúdo.
    @GetMapping("/api/fotos/{chave}")
    public ResponseEntity<byte[]> ler(@PathVariable String chave) {
        byte[] conteudo = fotos.ler(chave)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada."));
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(conteudo);
    }
}

package br.org.concentro.portal.cadastro;

import br.org.concentro.portal.acesso.EquipeStaff;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Perfil de quem está logado: leitura, edição dos dados permitidos e troca de
// senha. Exige o token: o id vem dele, então ninguém mexe no perfil de outra
// pessoa.
@RestController
public class PerfilController {

    private final CongressistaRepository congressistas;
    private final EquipeStaff staff;
    private final PerfilService perfis;

    public PerfilController(CongressistaRepository congressistas, EquipeStaff staff, PerfilService perfis) {
        this.congressistas = congressistas;
        this.staff = staff;
        this.perfis = perfis;
    }

    @GetMapping("/api/perfil")
    public PerfilResposta perfil(@AuthenticationPrincipal Jwt token) {
        Congressista c = congressistas.buscarComEj(id(token))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
        return resposta(c);
    }

    // Nome, cargo e área sempre; EJ e tempo de MEJ só antes da montagem (409 depois).
    @PutMapping("/api/perfil")
    public PerfilResposta atualizar(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody PerfilRequisicao requisicao) {
        return resposta(perfis.atualizar(id(token), requisicao));
    }

    // 400 se a senha atual estiver errada.
    @PutMapping("/api/perfil/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@AuthenticationPrincipal Jwt token, @Valid @RequestBody SenhaRequisicao requisicao) {
        perfis.trocarSenha(id(token), requisicao);
    }

    private PerfilResposta resposta(Congressista c) {
        return new PerfilResposta(c.getId(), c.getNome(), c.getEmail(), c.getEj().getId(),
            c.getEj().getNome(), c.getCargo(), c.getTempoMej(), c.getAreaAtuacao(),
            staff.ehStaff(c.getEmail()), c.getFotoChave(), perfis.podeMudarEjETempo());
    }

    private static Long id(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}

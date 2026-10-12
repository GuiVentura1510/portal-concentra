package br.org.concentro.portal.missao;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Aba Envios (retirada de envios) e marcação direta, no painel do staff.
@RestController
@RequestMapping("/api/staff")
public class StaffConclusaoController {

    private final ConclusaoService conclusoes;

    public StaffConclusaoController(ConclusaoService conclusoes) {
        this.conclusoes = conclusoes;
    }

    // Últimos envios aceitos e registros do staff (aba Envios).
    @GetMapping("/conclusoes")
    public List<ConclusaoResposta> envios() {
        return conclusoes.envios();
    }

    @PostMapping("/conclusoes/{id}/retirar")
    public ConclusaoResposta retirar(@PathVariable Long id, @AuthenticationPrincipal Jwt token,
            @Valid @RequestBody RecusaRequisicao requisicao) {
        return conclusoes.retirar(id, Long.valueOf(token.getSubject()), requisicao.motivo());
    }

    @PostMapping("/missoes/{id}/marcar")
    public ConclusaoResposta marcar(@PathVariable Long id, @AuthenticationPrincipal Jwt token,
            @Valid @RequestBody MarcarRequisicao requisicao) {
        return conclusoes.marcar(id, requisicao, Long.valueOf(token.getSubject()));
    }
}

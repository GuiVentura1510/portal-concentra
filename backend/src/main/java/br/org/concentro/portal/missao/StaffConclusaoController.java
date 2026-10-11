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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Fila de aprovação e marcação direta, no painel do staff.
@RestController
@RequestMapping("/api/staff")
public class StaffConclusaoController {

    private final ConclusaoService conclusoes;

    public StaffConclusaoController(ConclusaoService conclusoes) {
        this.conclusoes = conclusoes;
    }

    @GetMapping("/conclusoes")
    public List<ConclusaoResposta> fila(@RequestParam(defaultValue = "PENDENTE") StatusConclusao status) {
        return conclusoes.fila(status);
    }

    @PostMapping("/conclusoes/{id}/aprovar")
    public ConclusaoResposta aprovar(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        return conclusoes.aprovar(id, Long.valueOf(token.getSubject()));
    }

    @PostMapping("/conclusoes/{id}/recusar")
    public ConclusaoResposta recusar(@PathVariable Long id, @AuthenticationPrincipal Jwt token,
            @Valid @RequestBody RecusaRequisicao requisicao) {
        return conclusoes.recusar(id, Long.valueOf(token.getSubject()), requisicao.motivo());
    }

    @PostMapping("/missoes/{id}/marcar")
    public ConclusaoResposta marcar(@PathVariable Long id, @AuthenticationPrincipal Jwt token,
            @Valid @RequestBody MarcarRequisicao requisicao) {
        return conclusoes.marcar(id, requisicao, Long.valueOf(token.getSubject()));
    }
}

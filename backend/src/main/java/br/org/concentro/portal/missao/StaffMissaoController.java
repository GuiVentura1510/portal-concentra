package br.org.concentro.portal.missao;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Missões no painel do staff. Tudo em /api/staff exige o papel STAFF.
// Não há exclusão: para tirar uma missão do ar, o staff desmarca "visível".
@RestController
@RequestMapping("/api/staff/missoes")
public class StaffMissaoController {

    private final MissaoService missoes;

    public StaffMissaoController(MissaoService missoes) {
        this.missoes = missoes;
    }

    @GetMapping
    public List<MissaoResposta> todas() {
        return missoes.todas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MissaoResposta criar(@Valid @RequestBody MissaoRequisicao requisicao) {
        return missoes.criar(requisicao);
    }

    @PutMapping("/{id}")
    public MissaoResposta atualizar(@PathVariable Long id, @Valid @RequestBody MissaoRequisicao requisicao) {
        return missoes.atualizar(id, requisicao);
    }
}

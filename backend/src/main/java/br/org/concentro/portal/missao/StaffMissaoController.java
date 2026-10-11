package br.org.concentro.portal.missao;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Missões no painel do staff. Tudo em /api/staff exige o papel STAFF.
// A exclusão só vale para missões sem respostas nem pontos; as outras saem do ar
// desmarcando "visível".
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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        missoes.excluir(id);
    }
}

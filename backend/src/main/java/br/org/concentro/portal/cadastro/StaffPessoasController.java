package br.org.concentro.portal.cadastro;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Aba Pessoas do painel do staff. Tudo em /api/staff exige o papel STAFF.
@RestController
@RequestMapping("/api/staff/congressistas")
public class StaffPessoasController {

    private final StaffPessoasService pessoas;

    public StaffPessoasController(StaffPessoasService pessoas) {
        this.pessoas = pessoas;
    }

    @GetMapping
    public List<PessoaStaff> buscar(@RequestParam("busca") String busca) {
        return pessoas.buscar(busca);
    }

    @PostMapping("/{id}/senha")
    public SenhaTemporaria redefinirSenha(@PathVariable Long id) {
        return pessoas.redefinirSenha(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id, @AuthenticationPrincipal Jwt token) {
        pessoas.remover(id, Long.valueOf(token.getSubject()));
    }

    @PostMapping("/cruzamento")
    public List<PessoaStaff> cruzamento(@Valid @RequestBody CruzamentoRequisicao requisicao) {
        return pessoas.foraDaLista(requisicao.emails());
    }
}

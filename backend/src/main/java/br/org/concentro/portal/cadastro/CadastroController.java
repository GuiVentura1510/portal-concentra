package br.org.concentro.portal.cadastro;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Recebe o formulário de cadastro. Rota pública. Responde 201 quando cria,
// 400 para dados inválidos e 409 para e-mail já cadastrado.
@RestController
public class CadastroController {

    private final CadastroService cadastro;

    public CadastroController(CadastroService cadastro) {
        this.cadastro = cadastro;
    }

    @PostMapping("/api/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroResposta cadastrar(@Valid @RequestBody CadastroRequisicao requisicao) {
        return cadastro.cadastrar(requisicao);
    }
}

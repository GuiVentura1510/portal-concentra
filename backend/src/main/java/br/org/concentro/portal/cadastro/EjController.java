package br.org.concentro.portal.cadastro;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Lista as EJs para o formulário de cadastro. É rota pública, porque
// quem está se cadastrando ainda não tem conta.
@RestController
public class EjController {

    private final EjRepository ejs;

    // O Spring entrega o repositório pronto ao criar o controller.
    public EjController(EjRepository ejs) {
        this.ejs = ejs;
    }

    @GetMapping("/api/ejs")
    public List<EjResposta> listar() {
        return ejs.findAllByOrderByNomeAsc().stream()
            .map(ej -> new EjResposta(ej.getId(), ej.getNome()))
            .toList();
    }
}

package br.org.concentro.portal.acesso;

import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.cadastro.CongressistaRepository;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Troca e-mail e senha por um token. Rota pública. Responde 401 sem dizer
// se o erro foi no e-mail ou na senha, para não revelar quem tem conta.
@RestController
public class LoginController {

    private final CongressistaRepository congressistas;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public LoginController(CongressistaRepository congressistas, PasswordEncoder passwordEncoder,
            TokenService tokens) {
        this.congressistas = congressistas;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @PostMapping("/api/login")
    public LoginResposta login(@Valid @RequestBody LoginRequisicao requisicao) {
        ResponseStatusException negado =
            new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");

        // Senha acima do limite do BCrypt nunca foi aceita no cadastro.
        if (requisicao.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw negado;
        }
        String email = requisicao.email().trim().toLowerCase(Locale.ROOT);
        Congressista congressista = congressistas.findByEmail(email)
            .filter(c -> passwordEncoder.matches(requisicao.senha(), c.getSenhaHash()))
            .orElseThrow(() -> negado);
        return new LoginResposta(tokens.emitir(congressista), congressista.getNome());
    }
}

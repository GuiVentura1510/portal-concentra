package br.org.concentro.portal.cadastro;

import br.org.concentro.portal.tripulacao.TripulacaoRepository;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Edição do próprio perfil.
// - Nome, cargo e área: sempre.
// - EJ e tempo de MEJ: só antes da montagem das tripulações, porque a montagem
//   usa os dois (sem colegas da mesma EJ, mistura de novatos e veteranos).
// - E-mail: nunca pela pessoa.
// - Senha: com a senha atual.
@Service
public class PerfilService {

    // Mesmo limite do cadastro: o BCrypt ignora o que passa de 72 bytes.
    private static final int LIMITE_BYTES_SENHA = 72;

    private final CongressistaRepository congressistas;
    private final EjRepository ejs;
    private final TripulacaoRepository tripulacoes;
    private final PasswordEncoder passwordEncoder;

    public PerfilService(CongressistaRepository congressistas, EjRepository ejs,
            TripulacaoRepository tripulacoes, PasswordEncoder passwordEncoder) {
        this.congressistas = congressistas;
        this.ejs = ejs;
        this.tripulacoes = tripulacoes;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Congressista atualizar(Long id, PerfilRequisicao requisicao) {
        Congressista c = buscar(id);

        boolean mudaEjOuTempo = !Objects.equals(c.getEj().getId(), requisicao.ejId())
            || c.getTempoMej() != requisicao.tempoMej();
        if (mudaEjOuTempo) {
            // Montagem feita = existe ao menos uma tripulação.
            if (tripulacoes.count() > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "EJ e tempo de MEJ não mudam depois da montagem das tripulações.");
            }
            Ej ej = ejs.findById(requisicao.ejId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "EJ não encontrada."));
            c.atualizarEjETempo(ej, requisicao.tempoMej());
        }

        c.atualizarDados(requisicao.nome().trim(), requisicao.cargo(), requisicao.areaAtuacao());
        return c;
    }

    @Transactional
    public void trocarSenha(Long id, SenhaRequisicao requisicao) {
        Congressista c = buscar(id);
        if (!passwordEncoder.matches(requisicao.senhaAtual(), c.getSenhaHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }
        if (requisicao.novaSenha().getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_SENHA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha longa demais.");
        }
        c.trocarSenha(passwordEncoder.encode(requisicao.novaSenha()));
    }

    // Diz à tela se EJ e tempo de MEJ ainda podem mudar.
    public boolean podeMudarEjETempo() {
        return tripulacoes.count() == 0;
    }

    private Congressista buscar(Long id) {
        return congressistas.buscarComEj(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
    }
}

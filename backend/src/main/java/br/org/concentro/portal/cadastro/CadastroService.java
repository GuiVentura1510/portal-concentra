package br.org.concentro.portal.cadastro;

import java.nio.charset.StandardCharsets;
import br.org.concentro.portal.tripulacao.TripulacaoService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Regras do cadastro: e-mail em minúsculas e sem repetição, EJ existente
// e senha guardada só como hash.
@Service
public class CadastroService {

    // O BCrypt ignora o que passa de 72 bytes, e o Spring recusa senhas maiores.
    // Letras acentuadas ocupam 2 bytes, por isso a conferência é em bytes.
    private static final int LIMITE_BYTES_SENHA = 72;

    private final CongressistaRepository congressistas;
    private final EjRepository ejs;
    private final PasswordEncoder passwordEncoder;
    private final TripulacaoService tripulacoes;

    public CadastroService(CongressistaRepository congressistas, EjRepository ejs,
            PasswordEncoder passwordEncoder, TripulacaoService tripulacoes) {
        this.congressistas = congressistas;
        this.ejs = ejs;
        this.passwordEncoder = passwordEncoder;
        this.tripulacoes = tripulacoes;
    }

    @Transactional
    public CadastroResposta cadastrar(CadastroRequisicao requisicao) {
        String email = requisicao.email().trim().toLowerCase(Locale.ROOT);

        if (requisicao.senha().getBytes(StandardCharsets.UTF_8).length > LIMITE_BYTES_SENHA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha longa demais.");
        }
        if (congressistas.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        Ej ej = ejs.findById(requisicao.ejId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "EJ não encontrada."));

        Congressista novo = new Congressista(
            requisicao.nome().trim(), email, passwordEncoder.encode(requisicao.senha()), ej, requisicao.cargo(),
            requisicao.tempoMej(), requisicao.areaAtuacao());
        try {
            congressistas.saveAndFlush(novo);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros simultâneos com o mesmo e-mail: o banco barra o segundo.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }
        // Depois de 15/10, quem se cadastra já entra numa tripulação.
        tripulacoes.encaixarSeJaMontadas(novo);
        return new CadastroResposta(novo.getId(), novo.getNome(), novo.getEmail(), ej.getId());
    }
}

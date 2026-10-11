package br.org.concentro.portal.cadastro;

import br.org.concentro.portal.acesso.EquipeStaff;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Ações do staff sobre os cadastros: busca, senha esquecida, remoção e
// cruzamento com a lista de inscritos.
@Service
public class StaffPessoasService {

    // Sem letras e números que se confundem (0 e O, 1, l e I), para ditar em voz alta.
    private static final String ALFABETO = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int TAMANHO_SENHA = 8;
    private static final int MAXIMO_RESULTADOS = 50;

    private final CongressistaRepository congressistas;
    private final PasswordEncoder passwordEncoder;
    private final EquipeStaff staff;
    private final SecureRandom sorteio = new SecureRandom();

    public StaffPessoasService(CongressistaRepository congressistas, PasswordEncoder passwordEncoder,
            EquipeStaff staff) {
        this.congressistas = congressistas;
        this.passwordEncoder = passwordEncoder;
        this.staff = staff;
    }

    @Transactional(readOnly = true)
    public List<PessoaStaff> buscar(String texto) {
        String termo = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
        if (termo.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Digite pelo menos 2 letras.");
        }
        return congressistas.buscar("%" + termo + "%", PageRequest.of(0, MAXIMO_RESULTADOS)).stream()
            .map(this::paraPessoa).toList();
    }

    // Gera uma senha nova e devolve para o staff passar à pessoa.
    @Transactional
    public SenhaTemporaria redefinirSenha(Long id) {
        Congressista pessoa = buscarPorId(id);
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < TAMANHO_SENHA; i++) {
            senha.append(ALFABETO.charAt(sorteio.nextInt(ALFABETO.length())));
        }
        pessoa.trocarSenha(passwordEncoder.encode(senha));
        return new SenhaTemporaria(senha.toString());
    }

    // Remove um cadastro. Quem já participou de missões não pode ser removido,
    // porque o histórico de pontos depende dele.
    @Transactional
    public void remover(Long id, Long staffId) {
        if (id.equals(staffId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Você não pode remover a própria conta.");
        }
        Congressista pessoa = buscarPorId(id);
        if (congressistas.temHistorico(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta pessoa já participou de missões ou é capitã de uma tripulação; não pode ser removida.");
        }
        try {
            congressistas.delete(pessoa);
            congressistas.flush();
        } catch (DataIntegrityViolationException e) {
            // Alguém usou a conta entre a conferência acima e a remoção.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta pessoa já participou de missões ou é capitã de uma tripulação; não pode ser removida.");
        }
    }

    // Cadastros (fora o staff) cujo e-mail não está na lista de inscritos.
    @Transactional(readOnly = true)
    public List<PessoaStaff> foraDaLista(List<String> emailsDaLista) {
        Set<String> inscritos = emailsDaLista.stream()
            .filter(e -> e != null && !e.isBlank())
            .map(e -> e.trim().toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        return congressistas.todosComEj().stream()
            .filter(c -> !staff.ehStaff(c.getEmail()))
            .filter(c -> !inscritos.contains(c.getEmail()))
            .map(this::paraPessoa)
            .toList();
    }

    private Congressista buscarPorId(Long id) {
        return congressistas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
    }

    private PessoaStaff paraPessoa(Congressista c) {
        String tripulacao = c.getTripulacao() == null ? null : c.getTripulacao().getNome();
        return new PessoaStaff(c.getId(), c.getNome(), c.getEmail(), c.getEj().getNome(), c.getCargo(), tripulacao,
            staff.ehStaff(c.getEmail()));
    }
}

package br.org.concentro.portal.tripulacao;

import br.org.concentro.portal.acesso.EquipeStaff;
import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.cadastro.CongressistaRepository;
import br.org.concentro.portal.tripulacao.MontadorDeTripulacoes.Pessoa;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Montagem das tripulações, encaixe de quem chega depois e consultas.
@Service
public class TripulacaoService {

    private final TripulacaoRepository tripulacoes;
    private final CongressistaRepository congressistas;
    private final EquipeStaff staff;

    public TripulacaoService(TripulacaoRepository tripulacoes, CongressistaRepository congressistas,
            EquipeStaff staff) {
        this.tripulacoes = tripulacoes;
        this.congressistas = congressistas;
        this.staff = staff;
    }

    // Monta todas as tripulações de uma vez, com quem já se cadastrou.
    // Só pode rodar uma vez: depois disso, quem chega é encaixado no cadastro.
    @Transactional
    public MontagemResposta montar() {
        if (!tripulacoes.listarComTrava().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "As tripulações já foram montadas.");
        }
        List<Congressista> participantes = congressistas.semTripulacao().stream()
            .filter(c -> !staff.ehStaff(c.getEmail()))
            .toList();
        if (participantes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não há congressistas cadastrados.");
        }
        Map<Long, Congressista> porId = participantes.stream()
            .collect(Collectors.toMap(Congressista::getId, Function.identity()));

        // A semente fica registrada na resposta, para a montagem poder ser reproduzida.
        long semente = System.currentTimeMillis();
        List<List<Pessoa>> grupos =
            MontadorDeTripulacoes.montar(participantes.stream().map(TripulacaoService::paraPessoa).toList(), semente);

        int menor = Integer.MAX_VALUE;
        int maior = 0;
        int colegasDeMesmaEj = 0;
        for (int i = 0; i < grupos.size(); i++) {
            List<Pessoa> grupo = grupos.get(i);
            Tripulacao tripulacao = tripulacoes.save(new Tripulacao(NomesDeTripulacao.nome(i)));
            for (Pessoa pessoa : grupo) {
                porId.get(pessoa.id()).entrarNa(tripulacao);
            }
            menor = Math.min(menor, grupo.size());
            maior = Math.max(maior, grupo.size());
            colegasDeMesmaEj += grupo.size() - (int) grupo.stream().map(Pessoa::ejId).distinct().count();
        }
        return new MontagemResposta(grupos.size(), participantes.size(), menor, maior, colegasDeMesmaEj, semente);
    }

    // Chamado no cadastro. Antes da montagem não faz nada; depois, coloca a
    // pessoa na tripulação com vaga que não tem ninguém da mesma EJ.
    @Transactional
    public void encaixarSeJaMontadas(Congressista novo) {
        if (staff.ehStaff(novo.getEmail())) {
            return;
        }
        List<Tripulacao> todas = tripulacoes.listarComTrava();
        if (todas.isEmpty()) {
            return;
        }
        Map<Long, List<Pessoa>> membros = new LinkedHashMap<>();
        for (Tripulacao tripulacao : todas) {
            membros.put(tripulacao.getId(), new ArrayList<>());
        }
        for (Congressista c : congressistas.comTripulacao()) {
            List<Pessoa> grupo = membros.get(c.getTripulacao().getId());
            if (grupo != null) {
                grupo.add(paraPessoa(c));
            }
        }
        int posicao = MontadorDeTripulacoes.escolherGrupo(paraPessoa(novo), new ArrayList<>(membros.values()));
        novo.entrarNa(todas.get(posicao));
    }

    // Tripulação de quem está logado. 404 enquanto as tripulações não forem montadas.
    @Transactional(readOnly = true)
    public TripulacaoResposta daPessoa(Long congressistaId) {
        Congressista congressista = congressistas.findById(congressistaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
        Tripulacao tripulacao = congressista.getTripulacao();
        if (tripulacao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tripulação ainda não definida.");
        }
        return resposta(tripulacao);
    }

    // Números para o topo do painel do staff. Contas do staff não entram na conta.
    @Transactional(readOnly = true)
    public ResumoResposta resumo() {
        List<Congressista> participantes = congressistas.findAll().stream()
            .filter(c -> !staff.ehStaff(c.getEmail()))
            .toList();
        int comTripulacao = (int) participantes.stream().filter(c -> c.getTripulacao() != null).count();
        return new ResumoResposta(participantes.size(), comTripulacao, (int) tripulacoes.count());
    }

    // Todas as tripulações, para o staff.
    @Transactional(readOnly = true)
    public List<TripulacaoResposta> todas() {
        return tripulacoes.findAllByOrderByNomeAsc().stream().map(this::resposta).toList();
    }

    // Registra o capitão escolhido pela tripulação. Só vale alguém da própria tripulação.
    @Transactional
    public TripulacaoResposta definirCapitao(Long tripulacaoId, Long congressistaId) {
        Tripulacao tripulacao = buscar(tripulacaoId);
        Congressista capitao = congressistas.findById(congressistaId)
            .filter(c -> c.getTripulacao() != null && c.getTripulacao().getId().equals(tripulacaoId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Essa pessoa não é desta tripulação."));
        tripulacao.definirCapitao(capitao);
        return resposta(tripulacao);
    }

    // Define onde a tripulação se encontra. Texto vazio apaga.
    @Transactional
    public TripulacaoResposta definirPontoEncontro(Long tripulacaoId, String texto) {
        Tripulacao tripulacao = buscar(tripulacaoId);
        String limpo = texto == null ? "" : texto.trim();
        tripulacao.definirPontoEncontro(limpo.isEmpty() ? null : limpo);
        return resposta(tripulacao);
    }

    private Tripulacao buscar(Long id) {
        return tripulacoes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tripulação não encontrada."));
    }

    private TripulacaoResposta resposta(Tripulacao tripulacao) {
        List<TripulacaoResposta.Membro> membros = congressistas.membrosDa(tripulacao.getId()).stream()
            .map(m -> new TripulacaoResposta.Membro(m.getId(), m.getNome(), m.getEj().getNome(), m.getCargo(),
                m.getTempoMej(), m.getFotoChave()))
            .toList();
        Long capitaoId = tripulacao.getCapitao() == null ? null : tripulacao.getCapitao().getId();
        return new TripulacaoResposta(tripulacao.getId(), tripulacao.getNome(), tripulacao.getPontoEncontro(),
            capitaoId, membros);
    }

    // Sem tempo de MEJ (não deveria acontecer), a pessoa conta como novata da primeira faixa.
    private static Pessoa paraPessoa(Congressista c) {
        boolean novato = c.getTempoMej() == null || c.getTempoMej().ehNovato();
        int faixa = c.getTempoMej() == null ? 0 : c.getTempoMej().ordinal();
        return new Pessoa(c.getId(), c.getEj().getId(), novato, faixa);
    }
}

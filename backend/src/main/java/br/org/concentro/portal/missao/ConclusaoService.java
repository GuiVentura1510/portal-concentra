package br.org.concentro.portal.missao;

import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.cadastro.CongressistaRepository;
import br.org.concentro.portal.missao.MissaoDaTripulacao.MinhaConclusao;
import br.org.concentro.portal.tripulacao.Tripulacao;
import br.org.concentro.portal.tripulacao.TripulacaoRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// O jogo em si: a tripulação vê as missões, o capitão envia, o staff aprova,
// recusa ou marca direto, e cada aprovação vira um lançamento no extrato.
@Service
public class ConclusaoService {

    private final MissaoRepository missoes;
    private final ConclusaoRepository conclusoes;
    private final LancamentoRepository lancamentos;
    private final CongressistaRepository congressistas;
    private final TripulacaoRepository tripulacoes;

    public ConclusaoService(MissaoRepository missoes, ConclusaoRepository conclusoes,
            LancamentoRepository lancamentos, CongressistaRepository congressistas,
            TripulacaoRepository tripulacoes) {
        this.missoes = missoes;
        this.conclusoes = conclusoes;
        this.lancamentos = lancamentos;
        this.congressistas = congressistas;
        this.tripulacoes = tripulacoes;
    }

    // Missões visíveis, com a situação de cada uma para a tripulação de quem está logado.
    @Transactional(readOnly = true)
    public List<MissaoDaTripulacao> paraCongressista(Long congressistaId) {
        Congressista eu = buscarCongressista(congressistaId);
        Tripulacao tripulacao = eu.getTripulacao();

        // A conclusão mais recente de cada missão (a lista vem em ordem de envio).
        Map<Long, Conclusao> ultima = new HashMap<>();
        if (tripulacao != null) {
            for (Conclusao c : conclusoes.daTripulacao(tripulacao.getId())) {
                ultima.put(c.getMissao().getId(), c);
            }
        }
        boolean envio = podeEnviarPelaTripulacao(eu, tripulacao);
        Instant agora = Instant.now();

        return missoes.visiveisEmOrdem().stream().map(m -> {
            Conclusao c = ultima.get(m.getId());
            boolean cumprida = c != null && c.getStatus() == StatusConclusao.APROVADA;
            boolean ativa = c != null && c.getStatus() != StatusConclusao.RECUSADA;
            SituacaoMissao situacao = situacao(m, agora, cumprida);
            boolean possoEnviar = envio && m.getConclusao() == FormaConclusao.ENVIO_CAPITAO
                && situacao == SituacaoMissao.ABERTA && !ativa;
            Integer restantes = m.getVagas() == null ? null : Math.max(0, m.getVagas() - m.getVagasUsadas());
            MinhaConclusao minha = c == null ? null
                : new MinhaConclusao(c.getStatus(), c.getResposta(), c.getMotivoRecusa());
            return new MissaoDaTripulacao(m.getId(), m.getTitulo(), m.getDescricao(), m.getPontos(), m.getVagas(),
                restantes, m.getInicio(), m.getFim(), m.getConclusao(), situacao, minha, possoEnviar);
        }).toList();
    }

    // O capitão envia a resposta. Fica pendente até o staff avaliar.
    @Transactional
    public MinhaConclusao enviar(Long missaoId, Long congressistaId, String texto) {
        Congressista eu = buscarCongressista(congressistaId);
        Tripulacao tripulacao = eu.getTripulacao();
        if (tripulacao == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você ainda não tem tripulação.");
        }
        Missao missao = missoes.findById(missaoId).filter(Missao::isVisivel)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão não encontrada."));
        if (missao.getConclusao() != FormaConclusao.ENVIO_CAPITAO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta missão é registrada pelo staff.");
        }
        Instant agora = Instant.now();
        switch (situacao(missao, agora, false)) {
            case EM_BREVE -> throw new ResponseStatusException(HttpStatus.CONFLICT, "A missão ainda não começou.");
            case ENCERRADA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "O prazo desta missão acabou.");
            case ESGOTADA -> throw new ResponseStatusException(HttpStatus.CONFLICT, "As vagas desta missão acabaram.");
            case ABERTA -> {
                // Pode seguir.
            }
        }
        if (!podeEnviarPelaTripulacao(eu, tripulacao)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só o capitão envia as respostas da tripulação.");
        }
        if (conclusoes.existeAtiva(missao.getId(), tripulacao.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sua tripulação já enviou esta missão.");
        }
        Conclusao conclusao = Conclusao.enviada(missao, tripulacao, eu, texto.trim(), agora);
        try {
            conclusoes.saveAndFlush(conclusao);
        } catch (DataIntegrityViolationException e) {
            // Dois membros enviando ao mesmo tempo: o índice único do banco barra o segundo.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sua tripulação já enviou esta missão.");
        }
        return new MinhaConclusao(conclusao.getStatus(), conclusao.getResposta(), null);
    }

    @Transactional(readOnly = true)
    public List<ConclusaoResposta> fila(StatusConclusao status) {
        return conclusoes.comStatus(status).stream().map(ConclusaoResposta::de).toList();
    }

    // Aprova um envio: ocupa uma vaga (se houver limite) e lança os pontos.
    @Transactional
    public ConclusaoResposta aprovar(Long conclusaoId, Long staffId) {
        Conclusao conclusao = buscarPendente(conclusaoId);
        ocuparVaga(conclusao.getMissao().getId());
        // ocuparVaga limpa o contexto do JPA; a conclusão é lida de novo.
        conclusao = conclusoes.findById(conclusaoId).orElseThrow();
        Congressista staff = buscarCongressista(staffId);
        Instant agora = Instant.now();
        conclusao.aprovar(staff, agora);
        lancar(conclusao, staff, agora);
        return ConclusaoResposta.de(conclusao);
    }

    @Transactional
    public ConclusaoResposta recusar(Long conclusaoId, Long staffId, String motivo) {
        Conclusao conclusao = buscarPendente(conclusaoId);
        String limpo = motivo == null || motivo.isBlank() ? null : motivo.trim();
        conclusao.recusar(buscarCongressista(staffId), Instant.now(), limpo);
        return ConclusaoResposta.de(conclusao);
    }

    // O staff marca direto que a tripulação cumpriu (ex.: ao carimbar o passaporte).
    @Transactional
    public ConclusaoResposta marcar(Long missaoId, Long tripulacaoId, Long staffId) {
        if (!missoes.existsById(missaoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão não encontrada.");
        }
        if (!tripulacoes.existsById(tripulacaoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tripulação não encontrada.");
        }
        if (conclusoes.existeAtiva(missaoId, tripulacaoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta tripulação já cumpriu esta missão ou tem um envio esperando na fila.");
        }
        ocuparVaga(missaoId);
        // ocuparVaga limpa o contexto do JPA; tudo é lido depois dela.
        Missao missao = missoes.findById(missaoId).orElseThrow();
        Tripulacao tripulacao = tripulacoes.findById(tripulacaoId).orElseThrow();
        Congressista staff = buscarCongressista(staffId);
        Instant agora = Instant.now();
        Conclusao conclusao = Conclusao.marcadaPeloStaff(missao, tripulacao, staff, agora);
        try {
            conclusoes.saveAndFlush(conclusao);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta tripulação já cumpriu esta missão.");
        }
        lancar(conclusao, staff, agora);
        return ConclusaoResposta.de(conclusao);
    }

    // Sem capitão definido, qualquer membro envia; com capitão, só ele.
    private static boolean podeEnviarPelaTripulacao(Congressista eu, Tripulacao tripulacao) {
        if (tripulacao == null) {
            return false;
        }
        return tripulacao.getCapitao() == null || tripulacao.getCapitao().getId().equals(eu.getId());
    }

    static SituacaoMissao situacao(Missao m, Instant agora, boolean cumprida) {
        if (m.getInicio() != null && agora.isBefore(m.getInicio())) {
            return SituacaoMissao.EM_BREVE;
        }
        if (m.getFim() != null && agora.isAfter(m.getFim())) {
            return SituacaoMissao.ENCERRADA;
        }
        if (m.esgotada() && !cumprida) {
            return SituacaoMissao.ESGOTADA;
        }
        return SituacaoMissao.ABERTA;
    }

    private void ocuparVaga(Long missaoId) {
        if (missoes.ocuparVaga(missaoId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "As vagas desta missão acabaram.");
        }
    }

    private void lancar(Conclusao conclusao, Congressista staff, Instant agora) {
        try {
            lancamentos.saveAndFlush(new Lancamento(conclusao, staff, agora));
        } catch (DataIntegrityViolationException e) {
            // Dois staffs aprovando a mesma conclusão: só um lançamento vale.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conclusão já foi avaliada.");
        }
    }

    private Conclusao buscarPendente(Long conclusaoId) {
        Conclusao conclusao = conclusoes.findById(conclusaoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conclusão não encontrada."));
        if (conclusao.getStatus() != StatusConclusao.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta conclusão já foi avaliada.");
        }
        return conclusao;
    }

    private Congressista buscarCongressista(Long id) {
        return congressistas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
    }
}

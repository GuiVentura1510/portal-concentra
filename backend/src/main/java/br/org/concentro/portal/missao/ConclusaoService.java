package br.org.concentro.portal.missao;

import br.org.concentro.portal.acesso.EquipeStaff;
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

// O jogo em si: o congressista vê as missões, envia a resposta (o capitão, nas da
// tripulação; a própria pessoa, nas individuais), o staff aprova, recusa ou marca
// direto, e cada aprovação vira um lançamento no extrato.
@Service
public class ConclusaoService {

    private final MissaoRepository missoes;
    private final ConclusaoRepository conclusoes;
    private final LancamentoRepository lancamentos;
    private final CongressistaRepository congressistas;
    private final TripulacaoRepository tripulacoes;
    private final EquipeStaff equipe;

    public ConclusaoService(MissaoRepository missoes, ConclusaoRepository conclusoes,
            LancamentoRepository lancamentos, CongressistaRepository congressistas,
            TripulacaoRepository tripulacoes, EquipeStaff equipe) {
        this.missoes = missoes;
        this.conclusoes = conclusoes;
        this.lancamentos = lancamentos;
        this.congressistas = congressistas;
        this.tripulacoes = tripulacoes;
        this.equipe = equipe;
    }

    // Missões visíveis, com a situação de cada uma para quem está logado.
    @Transactional(readOnly = true)
    public List<MissaoDaTripulacao> paraCongressista(Long congressistaId) {
        Congressista eu = buscarCongressista(congressistaId);
        Tripulacao tripulacao = eu.getTripulacao();

        // A conclusão mais recente de cada missão (as listas vêm em ordem de envio).
        Map<Long, Conclusao> daTripulacao = new HashMap<>();
        if (tripulacao != null) {
            for (Conclusao c : conclusoes.daTripulacao(tripulacao.getId())) {
                daTripulacao.put(c.getMissao().getId(), c);
            }
        }
        Map<Long, Conclusao> minhas = new HashMap<>();
        for (Conclusao c : conclusoes.doCongressista(eu.getId())) {
            minhas.put(c.getMissao().getId(), c);
        }
        boolean envioPelaTripulacao = podeEnviarPelaTripulacao(eu, tripulacao);
        boolean staff = equipe.ehStaff(eu.getEmail());
        Instant agora = Instant.now();

        return missoes.visiveisEmOrdem().stream().map(m -> {
            Conclusao c = m.individual() ? minhas.get(m.getId()) : daTripulacao.get(m.getId());
            boolean cumprida = c != null && c.getStatus() == StatusConclusao.APROVADA;
            boolean ativa = c != null && c.getStatus() != StatusConclusao.RECUSADA;
            SituacaoMissao situacao = situacao(m, agora, cumprida);
            boolean posso = m.individual() ? !staff : envioPelaTripulacao;
            boolean possoEnviar = posso && m.getConclusao() == FormaConclusao.ENVIO_CAPITAO
                && situacao == SituacaoMissao.ABERTA && !ativa;
            Integer restantes = m.getVagas() == null ? null : Math.max(0, m.getVagas() - m.getVagasUsadas());
            MinhaConclusao minha = c == null ? null
                : new MinhaConclusao(c.getStatus(), c.getResposta(), c.getMotivoRecusa());
            return new MissaoDaTripulacao(m.getId(), m.getTitulo(), m.getDescricao(), m.getPontos(), m.getVagas(),
                restantes, m.getInicio(), m.getFim(), m.getConclusao(), m.getAlcance(), situacao, minha,
                possoEnviar);
        }).toList();
    }

    // Envio de uma resposta. Fica pendente até o staff avaliar.
    @Transactional
    public MinhaConclusao enviar(Long missaoId, Long congressistaId, String texto) {
        Congressista eu = buscarCongressista(congressistaId);
        if (equipe.ehStaff(eu.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contas do staff não participam das missões.");
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

        Conclusao conclusao;
        String jaEnviou;
        if (missao.individual()) {
            jaEnviou = "Você já enviou esta missão.";
            if (conclusoes.existeAtivaIndividual(missao.getId(), eu.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, jaEnviou);
            }
            conclusao = Conclusao.enviadaIndividual(missao, eu, texto.trim(), agora);
        } else {
            jaEnviou = "Sua tripulação já enviou esta missão.";
            Tripulacao tripulacao = eu.getTripulacao();
            if (tripulacao == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Você ainda não tem tripulação.");
            }
            if (!podeEnviarPelaTripulacao(eu, tripulacao)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Só o capitão envia as respostas da tripulação.");
            }
            if (conclusoes.existeAtiva(missao.getId(), tripulacao.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, jaEnviou);
            }
            conclusao = Conclusao.enviada(missao, tripulacao, eu, texto.trim(), agora);
        }
        try {
            conclusoes.saveAndFlush(conclusao);
        } catch (DataIntegrityViolationException e) {
            // Dois envios ao mesmo tempo: o índice único do banco barra o segundo.
            throw new ResponseStatusException(HttpStatus.CONFLICT, jaEnviou);
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

    // O staff marca direto que a tripulação (ou a pessoa, nas individuais) cumpriu,
    // por exemplo ao carimbar o passaporte.
    @Transactional
    public ConclusaoResposta marcar(Long missaoId, MarcarRequisicao alvo, Long staffId) {
        Missao encontrada = missoes.findById(missaoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão não encontrada."));
        return encontrada.individual()
            ? marcarPessoa(missaoId, alvo.congressistaId(), staffId)
            : marcarTripulacao(missaoId, alvo.tripulacaoId(), staffId);
    }

    private ConclusaoResposta marcarTripulacao(Long missaoId, Long tripulacaoId, Long staffId) {
        if (tripulacaoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta missão é da tripulação: escolha a tripulação.");
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
        return salvarMarcada(Conclusao.marcadaPeloStaff(missao, tripulacao, staff, agora), staff, agora,
            "Esta tripulação já cumpriu esta missão.");
    }

    private ConclusaoResposta marcarPessoa(Long missaoId, Long congressistaId, Long staffId) {
        if (congressistaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta missão é individual: escolha a pessoa.");
        }
        Congressista alvo = congressistas.findById(congressistaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada."));
        if (equipe.ehStaff(alvo.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contas do staff não participam das missões.");
        }
        if (conclusoes.existeAtivaIndividual(missaoId, congressistaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta pessoa já cumpriu esta missão ou tem um envio esperando na fila.");
        }
        ocuparVaga(missaoId);
        // ocuparVaga limpa o contexto do JPA; tudo é lido depois dela.
        Missao missao = missoes.findById(missaoId).orElseThrow();
        Congressista pessoa = congressistas.findById(congressistaId).orElseThrow();
        Congressista staff = buscarCongressista(staffId);
        Instant agora = Instant.now();
        return salvarMarcada(Conclusao.marcadaParaPessoa(missao, pessoa, staff, agora), staff, agora,
            "Esta pessoa já cumpriu esta missão.");
    }

    private ConclusaoResposta salvarMarcada(Conclusao conclusao, Congressista staff, Instant agora,
            String jaCumprida) {
        try {
            conclusoes.saveAndFlush(conclusao);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, jaCumprida);
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

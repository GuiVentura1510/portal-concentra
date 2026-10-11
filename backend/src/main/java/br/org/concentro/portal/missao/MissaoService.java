package br.org.concentro.portal.missao;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Cadastro de missões pelo staff.
@Service
public class MissaoService {

    private static final String TEM_HISTORICO =
        "Esta missão já tem respostas ou pontos lançados. Para tirá-la do ar, desmarque \"visível\".";

    private final MissaoRepository missoes;
    private final ConclusaoRepository conclusoes;
    private final LancamentoRepository lancamentos;

    public MissaoService(MissaoRepository missoes, ConclusaoRepository conclusoes,
            LancamentoRepository lancamentos) {
        this.missoes = missoes;
        this.conclusoes = conclusoes;
        this.lancamentos = lancamentos;
    }

    @Transactional(readOnly = true)
    public List<MissaoResposta> todas() {
        return missoes.todasEmOrdem().stream().map(MissaoResposta::de).toList();
    }

    @Transactional
    public MissaoResposta criar(MissaoRequisicao r) {
        validarJanela(r);
        AlcanceMissao alcance = r.alcance() == null ? AlcanceMissao.TRIPULACAO : r.alcance();
        Missao missao = new Missao(r.titulo().trim(), descricao(r), r.pontos(), r.vagas(), instante(r.inicio()),
            instante(r.fim()), r.conclusao(), r.visivel(), alcance);
        return MissaoResposta.de(missoes.save(missao));
    }

    @Transactional
    public MissaoResposta atualizar(Long id, MissaoRequisicao r) {
        validarJanela(r);
        Missao missao = buscar(id);
        AlcanceMissao alcance = r.alcance() == null ? missao.getAlcance() : r.alcance();
        // Trocar entre individual e tripulação com respostas já dadas misturaria as regras.
        if (alcance != missao.getAlcance() && temHistorico(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta missão já tem respostas: não dá para trocar entre individual e da tripulação.");
        }
        missao.atualizar(r.titulo().trim(), descricao(r), r.pontos(), r.vagas(), instante(r.inicio()),
            instante(r.fim()), r.conclusao(), r.visivel(), alcance);
        return MissaoResposta.de(missao);
    }

    // Exclui uma missão que ninguém respondeu. Com respostas ou pontos, recusa:
    // apagar sumiria com pontos do ranking.
    @Transactional
    public void excluir(Long id) {
        Missao missao = buscar(id);
        if (temHistorico(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, TEM_HISTORICO);
        }
        try {
            missoes.delete(missao);
            missoes.flush();
        } catch (DataIntegrityViolationException e) {
            // Alguém respondeu entre a consulta e a exclusão: o banco barra.
            throw new ResponseStatusException(HttpStatus.CONFLICT, TEM_HISTORICO);
        }
    }

    private boolean temHistorico(Long id) {
        return conclusoes.existsByMissaoId(id) || lancamentos.existsByMissaoId(id);
    }

    private Missao buscar(Long id) {
        return missoes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão não encontrada."));
    }

    private static void validarJanela(MissaoRequisicao r) {
        if (r.inicio() != null && r.fim() != null && !r.fim().isAfter(r.inicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O fim precisa ser depois do início.");
        }
    }

    private static String descricao(MissaoRequisicao r) {
        return r.descricao() == null ? "" : r.descricao().trim();
    }

    private static Instant instante(OffsetDateTime horario) {
        return horario == null ? null : horario.toInstant();
    }
}

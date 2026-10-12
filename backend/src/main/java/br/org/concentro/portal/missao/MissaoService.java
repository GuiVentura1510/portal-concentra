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
        "Esta missão tem envios valendo pontos. Retire-os na aba Envios ou desmarque \"visível\".";

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

    // Exclui uma missão sem envios valendo pontos (decidido em 12/10). Envios que o
    // staff já retirou não impedem: são apagados junto. Com envio ativo ou pontos,
    // recusa, porque apagar sumiria com pontos do ranking.
    @Transactional
    public void excluir(Long id) {
        buscar(id);
        if (temEnvioAtivo(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, TEM_HISTORICO);
        }
        try {
            conclusoes.apagarRetiradasDaMissao(id);
            missoes.deleteById(id);
            missoes.flush();
        } catch (DataIntegrityViolationException e) {
            // Alguém enviou entre a consulta e a exclusão: o banco barra e nada é apagado.
            throw new ResponseStatusException(HttpStatus.CONFLICT, TEM_HISTORICO);
        }
    }

    // Envio pendente ou aceito, ou ponto lançado: impede a exclusão.
    private boolean temEnvioAtivo(Long id) {
        return conclusoes.existsByMissaoIdAndStatusIn(id, ConclusaoRepository.ATIVAS)
            || lancamentos.existsByMissaoId(id);
    }

    // Qualquer resposta, mesmo retirada: impede trocar o alcance.
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

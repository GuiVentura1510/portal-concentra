package br.org.concentro.portal.missao;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Cadastro de missões pelo staff.
@Service
public class MissaoService {

    private final MissaoRepository missoes;

    public MissaoService(MissaoRepository missoes) {
        this.missoes = missoes;
    }

    @Transactional(readOnly = true)
    public List<MissaoResposta> todas() {
        return missoes.todasEmOrdem().stream().map(MissaoResposta::de).toList();
    }

    @Transactional
    public MissaoResposta criar(MissaoRequisicao r) {
        validarJanela(r);
        Missao missao = new Missao(r.titulo().trim(), descricao(r), r.pontos(), r.vagas(), instante(r.inicio()),
            instante(r.fim()), r.conclusao(), r.visivel());
        return MissaoResposta.de(missoes.save(missao));
    }

    @Transactional
    public MissaoResposta atualizar(Long id, MissaoRequisicao r) {
        validarJanela(r);
        Missao missao = missoes.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão não encontrada."));
        missao.atualizar(r.titulo().trim(), descricao(r), r.pontos(), r.vagas(), instante(r.inicio()),
            instante(r.fim()), r.conclusao(), r.visivel());
        return MissaoResposta.de(missao);
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

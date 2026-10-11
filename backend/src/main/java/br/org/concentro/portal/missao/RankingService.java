package br.org.concentro.portal.missao;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Ranking das tripulações pela soma do extrato. Fica guardado em memória por
// alguns segundos (regra do README), porque no evento centenas de celulares
// vão abrir o ranking ao mesmo tempo e o resultado muda pouco de um segundo para outro.
@Service
public class RankingService {

    private record Calculado(Instant em, List<PosicaoRanking> posicoes) {
    }

    private final LancamentoRepository lancamentos;
    private final Duration validade;
    private final AtomicReference<Calculado> guardado = new AtomicReference<>();

    public RankingService(LancamentoRepository lancamentos, @Value("${app.ranking.cache:PT15S}") Duration validade) {
        this.lancamentos = lancamentos;
        this.validade = validade;
    }

    @Transactional(readOnly = true)
    public List<PosicaoRanking> ranking() {
        Instant agora = Instant.now();
        Calculado atual = guardado.get();
        if (atual != null && agora.isBefore(atual.em().plus(validade))) {
            return atual.posicoes();
        }
        List<PosicaoRanking> novo = calcular();
        guardado.set(new Calculado(agora, novo));
        return novo;
    }

    private List<PosicaoRanking> calcular() {
        List<Object[]> linhas = lancamentos.placar();
        List<PosicaoRanking> posicoes = new ArrayList<>();
        long pontosAnteriores = Long.MIN_VALUE;
        int posicao = 0;
        for (int i = 0; i < linhas.size(); i++) {
            Object[] linha = linhas.get(i);
            long pontos = ((Number) linha[2]).longValue();
            // Empate divide a posição: 1, 2, 2, 4.
            if (pontos != pontosAnteriores) {
                posicao = i + 1;
                pontosAnteriores = pontos;
            }
            posicoes.add(new PosicaoRanking(posicao, ((Number) linha[0]).longValue(), (String) linha[1], pontos,
                ((Number) linha[3]).longValue()));
        }
        return List.copyOf(posicoes);
    }
}

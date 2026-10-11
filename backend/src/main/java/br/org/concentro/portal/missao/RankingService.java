package br.org.concentro.portal.missao;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Os dois rankings, pela soma do extrato:
// - o das tripulações, só com as missões da tripulação;
// - o individual, só com as missões individuais.
// Cada um fica guardado em memória por alguns segundos (regra do README), porque no
// evento centenas de celulares vão abrir o ranking ao mesmo tempo.
@Service
public class RankingService {

    // Quantas posições do ranking individual o site mostra.
    static final int LIMITE_INDIVIDUAL = 100;

    private record Calculado<T>(Instant em, List<T> posicoes) {
    }

    private final LancamentoRepository lancamentos;
    private final Duration validade;
    private final AtomicReference<Calculado<PosicaoRanking>> tripulacoes = new AtomicReference<>();
    private final AtomicReference<Calculado<PosicaoIndividual>> individual = new AtomicReference<>();

    public RankingService(LancamentoRepository lancamentos, @Value("${app.ranking.cache:PT15S}") Duration validade) {
        this.lancamentos = lancamentos;
        this.validade = validade;
    }

    @Transactional(readOnly = true)
    public List<PosicaoRanking> ranking() {
        return guardado(tripulacoes, this::calcularTripulacoes);
    }

    @Transactional(readOnly = true)
    public RankingIndividual individual(Long congressistaId) {
        List<PosicaoIndividual> todas = guardado(individual, this::calcularIndividual);
        PosicaoIndividual minha = todas.stream()
            .filter(p -> p.congressistaId().equals(congressistaId))
            .findFirst().orElse(null);
        return new RankingIndividual(todas.stream().limit(LIMITE_INDIVIDUAL).toList(), minha, todas.size());
    }

    // Devolve o ranking guardado enquanto ele for válido; senão, calcula de novo.
    private <T> List<T> guardado(AtomicReference<Calculado<T>> ref, Supplier<List<T>> calcular) {
        Instant agora = Instant.now();
        Calculado<T> atual = ref.get();
        if (atual != null && agora.isBefore(atual.em().plus(validade))) {
            return atual.posicoes();
        }
        List<T> novo = calcular.get();
        ref.set(new Calculado<>(agora, novo));
        return novo;
    }

    private List<PosicaoRanking> calcularTripulacoes() {
        List<Object[]> linhas = lancamentos.placar();
        int[] posicoes = posicoes(linhas, 2);
        List<PosicaoRanking> lista = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            Object[] l = linhas.get(i);
            lista.add(new PosicaoRanking(posicoes[i], numero(l[0]), (String) l[1], numero(l[2]), numero(l[3])));
        }
        return List.copyOf(lista);
    }

    private List<PosicaoIndividual> calcularIndividual() {
        List<Object[]> linhas = lancamentos.placarIndividual();
        int[] posicoes = posicoes(linhas, 4);
        List<PosicaoIndividual> lista = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i++) {
            Object[] l = linhas.get(i);
            lista.add(new PosicaoIndividual(posicoes[i], numero(l[0]), (String) l[1], (String) l[2], (String) l[3],
                numero(l[4]), numero(l[5])));
        }
        return List.copyOf(lista);
    }

    // Posição de cada linha (já em ordem de pontos). Empate divide a posição: 1, 2, 2, 4.
    private static int[] posicoes(List<Object[]> linhas, int colunaPontos) {
        int[] resultado = new int[linhas.size()];
        long anteriores = Long.MIN_VALUE;
        int posicao = 0;
        for (int i = 0; i < linhas.size(); i++) {
            long pontos = numero(linhas.get(i)[colunaPontos]);
            if (pontos != anteriores) {
                posicao = i + 1;
                anteriores = pontos;
            }
            resultado[i] = posicao;
        }
        return resultado;
    }

    private static long numero(Object valor) {
        return ((Number) valor).longValue();
    }
}

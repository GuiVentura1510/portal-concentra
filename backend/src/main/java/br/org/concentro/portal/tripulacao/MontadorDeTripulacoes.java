package br.org.concentro.portal.tripulacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

// Regras de montagem das tripulações, sem banco nem Spring, para poder ser
// testada isoladamente. Segue o documento do evento: grupos de 5 a 6 pessoas,
// de EJs diferentes, misturando novatos (menos de 1 ano de MEJ) e veteranos e,
// dentro disso, as quatro faixas de tempo de MEJ.
public final class MontadorDeTripulacoes {

    public static final int TAMANHO_MAXIMO = 6;

    // O mínimo de dados que o algoritmo precisa de cada pessoa.
    // "faixa" é a faixa de tempo de MEJ (0 = menos de 6 meses ... 3 = mais de 2 anos).
    public record Pessoa(long id, long ejId, boolean novato, int faixa) {
    }

    private MontadorDeTripulacoes() {
    }

    // Divide todas as pessoas em grupos. A semente torna o sorteio reproduzível:
    // a mesma lista com a mesma semente gera os mesmos grupos.
    public static List<List<Pessoa>> montar(List<Pessoa> pessoas, long semente) {
        if (pessoas.isEmpty()) {
            return List.of();
        }
        int quantidade = (pessoas.size() + TAMANHO_MAXIMO - 1) / TAMANHO_MAXIMO;
        // Limite por grupo que distribui as pessoas por igual (ex.: 61 pessoas em
        // 11 grupos dá limite 6, e os grupos ficam com 5 ou 6).
        int limite = (pessoas.size() + quantidade - 1) / quantidade;

        List<List<Pessoa>> grupos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            grupos.add(new ArrayList<>());
        }

        // EJs com mais gente entram primeiro: são as mais difíceis de espalhar.
        Map<Long, Long> tamanhoDaEj = pessoas.stream()
            .collect(Collectors.groupingBy(Pessoa::ejId, Collectors.counting()));
        List<Pessoa> ordem = new ArrayList<>(pessoas);
        Collections.shuffle(ordem, new Random(semente));
        ordem.sort(Comparator.comparing((Pessoa p) -> -tamanhoDaEj.get(p.ejId()))
            .thenComparing(Pessoa::ejId));

        for (Pessoa pessoa : ordem) {
            List<List<Pessoa>> comVaga = grupos.stream().filter(g -> g.size() < limite).toList();
            melhorGrupo(pessoa, comVaga).add(pessoa);
        }
        return grupos;
    }

    // Escolhe a tripulação de quem se cadastrou depois da montagem.
    // Devolve a posição do grupo na lista recebida.
    public static int escolherGrupo(Pessoa pessoa, List<List<Pessoa>> grupos) {
        if (grupos.isEmpty()) {
            throw new IllegalArgumentException("Não há tripulações montadas.");
        }
        List<List<Pessoa>> comVaga = grupos.stream().filter(g -> g.size() < TAMANHO_MAXIMO).toList();
        List<Pessoa> escolhido = melhorGrupo(pessoa, comVaga.isEmpty() ? grupos : comVaga);
        return grupos.indexOf(escolhido);
    }

    // Critérios, em ordem de importância:
    // 1. menos colegas da mesma EJ (o ideal é nenhum);
    // 2. menos pessoas, para os grupos crescerem por igual;
    // 3. menos pessoas do mesmo nível (novato ou veterano), para misturar;
    // 4. menos pessoas da mesma faixa de tempo de MEJ, para espalhar as quatro faixas.
    private static List<Pessoa> melhorGrupo(Pessoa pessoa, List<List<Pessoa>> candidatos) {
        Function<List<Pessoa>, Long> mesmaEj =
            g -> g.stream().filter(o -> o.ejId() == pessoa.ejId()).count();
        Function<List<Pessoa>, Long> mesmoNivel =
            g -> g.stream().filter(o -> o.novato() == pessoa.novato()).count();
        Function<List<Pessoa>, Long> mesmaFaixa =
            g -> g.stream().filter(o -> o.faixa() == pessoa.faixa()).count();
        return candidatos.stream()
            .min(Comparator.comparing(mesmaEj)
                .thenComparing(g -> (long) g.size())
                .thenComparing(mesmoNivel)
                .thenComparing(mesmaFaixa))
            .orElseThrow();
    }
}

package br.org.concentro.portal.tripulacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.org.concentro.portal.tripulacao.MontadorDeTripulacoes.Pessoa;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// Testes só do algoritmo, sem banco: rodam em milissegundos.
class MontadorDeTripulacoesTests {

    // Pessoa com faixa de tempo de MEJ de 0 a 3; as faixas 0 e 1 são de novatos.
    private static Pessoa pessoa(long id, long ejId, int faixa) {
        return new Pessoa(id, ejId, faixa < 2, faixa);
    }

    // n pessoas espalhadas por "ejs" EJs, passando pelas quatro faixas.
    private static List<Pessoa> pessoas(int n, int ejs) {
        List<Pessoa> lista = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            lista.add(pessoa(i, i % ejs, i % 4));
        }
        return lista;
    }

    private static long distintas(List<Pessoa> grupo) {
        return grupo.stream().map(Pessoa::ejId).distinct().count();
    }

    @Test
    void sessentaPessoasViramDezTripulacoesDeSeisSemColegaDaMesmaEj() {
        List<List<Pessoa>> grupos = MontadorDeTripulacoes.montar(pessoas(60, 12), 1L);

        assertThat(grupos).hasSize(10);
        for (List<Pessoa> grupo : grupos) {
            assertThat(grupo).hasSize(6);
            assertThat(distintas(grupo)).isEqualTo(6);
            // Mistura novatos e veteranos, e aqui cabem as quatro faixas.
            assertThat(grupo).anyMatch(Pessoa::novato).anyMatch(p -> !p.novato());
            assertThat(grupo.stream().map(Pessoa::faixa).distinct().count()).isEqualTo(4);
        }
    }

    @Test
    void distribuiPorIgualQuandoNaoDivideExato() {
        List<List<Pessoa>> grupos = MontadorDeTripulacoes.montar(pessoas(61, 20), 7L);

        assertThat(grupos).hasSize(11);
        assertThat(grupos).allSatisfy(g -> assertThat(g.size()).isBetween(5, 6));
        assertThat(grupos.stream().mapToInt(List::size).sum()).isEqualTo(61);
    }

    @Test
    void soRepeteEjQuandoAEjTemMaisGenteQueTripulacoes() {
        // 30 pessoas = 5 tripulações. A EJ 0 tem 8 pessoas: 3 repetições são inevitáveis.
        List<Pessoa> lista = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            lista.add(pessoa(i, 0, i % 4));
        }
        for (int i = 8; i < 30; i++) {
            lista.add(pessoa(i, i, i % 4));
        }
        List<List<Pessoa>> grupos = MontadorDeTripulacoes.montar(lista, 3L);

        int repeticoes = grupos.stream().mapToInt(g -> g.size() - (int) distintas(g)).sum();
        assertThat(repeticoes).isEqualTo(3);
    }

    @Test
    void mesmaSementeGeraAsMesmasTripulacoes() {
        List<Pessoa> lista = pessoas(40, 9);
        assertThat(MontadorDeTripulacoes.montar(lista, 99L)).isEqualTo(MontadorDeTripulacoes.montar(lista, 99L));
    }

    @Test
    void quemChegaDepoisVaiParaTripulacaoComVagaESemColegaDaEj() {
        List<List<Pessoa>> grupos = List.of(
            new ArrayList<>(List.of(pessoa(1, 10, 0), pessoa(2, 20, 3))),
            new ArrayList<>(List.of(pessoa(3, 30, 0))),
            new ArrayList<>(List.of(pessoa(4, 40, 0), pessoa(5, 50, 3))));

        // EJ 30 já está no grupo 1, que é o menor; o grupo 0 empata com o 2 e vem antes.
        assertThat(MontadorDeTripulacoes.escolherGrupo(pessoa(6, 30, 3), grupos)).isEqualTo(0);
        // Sem conflito de EJ, vai para o menor.
        assertThat(MontadorDeTripulacoes.escolherGrupo(pessoa(7, 60, 3), grupos)).isEqualTo(1);
    }

    @Test
    void nomesDasTripulacoesNaoSeRepetem() {
        assertThat(NomesDeTripulacao.CONSTELACOES).doesNotHaveDuplicates().hasSize(88);
        assertThat(NomesDeTripulacao.nome(0)).isEqualTo("Órion");
        assertThat(NomesDeTripulacao.nome(88)).isEqualTo("Órion 2");
    }
}

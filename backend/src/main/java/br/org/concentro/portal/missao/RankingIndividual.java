package br.org.concentro.portal.missao;

import java.util.List;

// Ranking individual como o site mostra: as primeiras posições, a posição de quem
// está logado (vazia se ainda não pontuou) e quantas pessoas já pontuaram.
public record RankingIndividual(List<PosicaoIndividual> posicoes, PosicaoIndividual minha, int total) {
}

package br.org.concentro.portal.missao;

// Uma linha do ranking. Tripulações empatadas dividem a posição (1, 2, 2, 4...).
public record PosicaoRanking(int posicao, Long tripulacaoId, String nome, long pontos, long missoesCumpridas) {
}

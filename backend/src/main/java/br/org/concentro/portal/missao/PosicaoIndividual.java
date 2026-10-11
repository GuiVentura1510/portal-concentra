package br.org.concentro.portal.missao;

// Uma linha do ranking individual. Pessoas empatadas dividem a posição (1, 2, 2, 4...).
public record PosicaoIndividual(int posicao, Long congressistaId, String nome, String ejNome,
    String tripulacaoNome, long pontos, long missoesCumpridas) {
}

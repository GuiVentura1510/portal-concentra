package br.org.concentro.portal.missao;

// Uma linha do ranking individual. Pessoas empatadas dividem a posição (1, 2, 2, 4...).
// "fotoChave" forma o endereço da foto de perfil; nula para quem não enviou foto.
public record PosicaoIndividual(int posicao, Long congressistaId, String nome, String ejNome,
    String tripulacaoNome, long pontos, long missoesCumpridas, String fotoChave) {
}

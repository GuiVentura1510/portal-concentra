package br.org.concentro.portal.tripulacao;

// Resumo da montagem, para o staff conferir o resultado.
// "colegasDeMesmaEj" só passa de zero quando uma EJ tem mais gente do que há tripulações.
public record MontagemResposta(
    int tripulacoes,
    int pessoas,
    int menorTripulacao,
    int maiorTripulacao,
    int colegasDeMesmaEj,
    long semente) {
}

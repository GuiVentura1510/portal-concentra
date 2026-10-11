package br.org.concentro.portal.cadastro;

// Opções da pergunta "há quanto tempo você está no MEJ?".
// Os nomes são gravados no banco como texto e precisam bater com a V2.
public enum TempoMej {
    MENOS_DE_6_MESES,
    DE_6_MESES_A_1_ANO,
    DE_1_A_2_ANOS,
    MAIS_DE_2_ANOS;

    // Novato é quem tem menos de 1 ano de MEJ, como no documento do evento.
    // As tripulações misturam novatos com veteranos.
    public boolean ehNovato() {
        return this == MENOS_DE_6_MESES || this == DE_6_MESES_A_1_ANO;
    }
}

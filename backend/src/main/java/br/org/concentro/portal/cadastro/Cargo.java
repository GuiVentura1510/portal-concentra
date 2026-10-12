package br.org.concentro.portal.cadastro;

// Opções da pergunta "qual o seu cargo na EJ?", feita no cadastro.
// Os nomes são gravados no banco como texto e precisam bater com a V9.
public enum Cargo {
    TRAINEE,
    ASSESSOR_CONSULTOR,
    PROJETISTA,
    GERENTE_COORDENADOR,
    DIRETOR,
    PRESIDENTE,
    POS_JUNIOR;

    // Trainee ainda não atua numa área da EJ, por isso não responde a pergunta.
    public boolean temAreaDeAtuacao() {
        return this != TRAINEE;
    }
}

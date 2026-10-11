package br.org.concentro.portal.missao;

// Como uma missão é concluída. Os nomes precisam bater com a regra da V5.
public enum FormaConclusao {
    // O capitão envia uma resposta pelo portal e o staff aprova ou recusa.
    ENVIO_CAPITAO,
    // O staff marca como cumprida direto, por exemplo ao carimbar o passaporte.
    STAFF_MARCA
}

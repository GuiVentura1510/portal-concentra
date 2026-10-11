package br.org.concentro.portal.missao;

// Como uma missão é concluída. Os nomes precisam bater com a regra da V5.
public enum FormaConclusao {
    // Uma resposta é enviada pelo portal e o staff aprova ou recusa. Nas missões da
    // tripulação, quem envia é o capitão; nas individuais, a própria pessoa.
    ENVIO_CAPITAO,
    // O staff marca como cumprida direto, por exemplo ao carimbar o passaporte.
    STAFF_MARCA
}

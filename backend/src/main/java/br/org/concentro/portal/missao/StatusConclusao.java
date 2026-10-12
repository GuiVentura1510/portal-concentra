package br.org.concentro.portal.missao;

// Situação de uma conclusão. Os nomes precisam bater com a regra da V6.
public enum StatusConclusao {
    // Enviada e esperando o staff. Sem uso desde 12/10, quando os envios passaram
    // a ser aceitos na hora; continua aqui porque o banco (V6) aceita o valor.
    PENDENTE,
    // Valeu pontos: envio aceito na hora ou registro do staff.
    APROVADA,
    // Retirada pelo staff (antes de 12/10: recusada na fila). Não vale pontos, e a
    // tripulação ou a pessoa pode enviar de novo.
    RECUSADA
}

package br.org.concentro.portal.missao;

// Situação de uma conclusão. Os nomes precisam bater com a regra da V6.
public enum StatusConclusao {
    // Enviada pelo capitão, esperando o staff.
    PENDENTE,
    // Valeu pontos.
    APROVADA,
    // Não valeu; a tripulação pode enviar de novo.
    RECUSADA
}

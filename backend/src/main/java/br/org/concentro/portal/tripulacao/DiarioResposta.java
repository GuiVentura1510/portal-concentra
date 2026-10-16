package br.org.concentro.portal.tripulacao;

import java.util.List;

// Diário de Bordo da tripulação: as fotos anexadas e quantas cabem no total.
public record DiarioResposta(int limite, List<FotoDiarioResposta> fotos) {
}

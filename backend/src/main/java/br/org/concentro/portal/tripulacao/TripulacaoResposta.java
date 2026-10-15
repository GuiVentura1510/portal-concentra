package br.org.concentro.portal.tripulacao;

import br.org.concentro.portal.cadastro.Cargo;
import br.org.concentro.portal.cadastro.TempoMej;
import java.util.List;

// Uma tripulação como a API mostra: nome, ponto de encontro, capitão e membros.
public record TripulacaoResposta(
    Long id,
    String nome,
    String pontoEncontro,
    Long capitaoId,
    List<Membro> membros) {

    // "fotoChave" forma o endereço da foto de perfil; nula para quem não enviou foto.
    // "tempoMej" aparece na aba Tripulação, para os colegas se conhecerem.
    public record Membro(Long id, String nome, String ejNome, Cargo cargo, TempoMej tempoMej, String fotoChave) {
    }
}

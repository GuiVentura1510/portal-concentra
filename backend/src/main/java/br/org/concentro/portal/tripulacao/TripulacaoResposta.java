package br.org.concentro.portal.tripulacao;

import br.org.concentro.portal.cadastro.Cargo;
import java.util.List;

// Uma tripulação como a API mostra: nome, ponto de encontro, capitão e membros.
public record TripulacaoResposta(
    Long id,
    String nome,
    String pontoEncontro,
    Long capitaoId,
    List<Membro> membros) {

    // "fotoChave" forma o endereço da foto de perfil; nula para quem não enviou foto.
    public record Membro(Long id, String nome, String ejNome, Cargo cargo, String fotoChave) {
    }
}

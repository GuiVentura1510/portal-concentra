package br.org.concentro.portal.tripulacao;

import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.cadastro.CongressistaRepository;
import br.org.concentro.portal.infra.Jpeg;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Diário de Bordo: fotos que os integrantes anexam do celular, sem valer pontos.
// Cada tripulação vê só o próprio diário. Quem anexou pode apagar a sua; o staff
// pode apagar qualquer uma (foto inadequada, por exemplo).
@Service
public class DiarioService {

    // Fotos por tripulação: umas 3 por integrante, e o banco fica limitado (cerca de
    // 200 KB por foto, uns 4 MB por tripulação no máximo).
    static final int LIMITE_POR_TRIPULACAO = 20;

    // Mesmo limite das fotos de missão: o site reduz para 1280 px (100 a 250 KB).
    static final int LIMITE_FOTO_BYTES = 400 * 1024;

    // Quantas fotos a aba Diário do staff mostra de uma vez.
    static final int LIMITE_STAFF = 300;

    private final FotoDiarioRepository fotos;
    private final TripulacaoRepository tripulacoes;
    private final CongressistaRepository congressistas;

    public DiarioService(FotoDiarioRepository fotos, TripulacaoRepository tripulacoes,
            CongressistaRepository congressistas) {
        this.fotos = fotos;
        this.tripulacoes = tripulacoes;
        this.congressistas = congressistas;
    }

    // Diário da tripulação de quem está logado. 404 antes da montagem e para o staff.
    @Transactional(readOnly = true)
    public DiarioResposta listar(Long congressistaId) {
        Tripulacao tripulacao = tripulacaoDe(buscarCongressista(congressistaId));
        return new DiarioResposta(LIMITE_POR_TRIPULACAO, fotos.daTripulacao(tripulacao.getId()));
    }

    // Anexa uma foto ao diário da própria tripulação, com a legenda que a pessoa
    // escreveu (opcional). Qualquer integrante pode.
    @Transactional
    public FotoDiarioResposta anexar(Long congressistaId, String imagemBase64, String legenda) {
        byte[] conteudo = Jpeg.decodificar(imagemBase64, LIMITE_FOTO_BYTES, "Foto grande demais.");
        String limpa = legenda == null || legenda.isBlank() ? null : legenda.trim();
        Congressista eu = buscarCongressista(congressistaId);
        Tripulacao tripulacao = tripulacoes.buscarComTrava(tripulacaoDe(eu).getId()).orElseThrow();
        if (fotos.contarDaTripulacao(tripulacao.getId()) >= LIMITE_POR_TRIPULACAO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "O diário já tem " + LIMITE_POR_TRIPULACAO + " fotos. Apague uma para anexar outra.");
        }
        FotoDiario foto = fotos.saveAndFlush(
            new FotoDiario(tripulacao, eu, UUID.randomUUID().toString(), conteudo, limpa, Instant.now()));
        return new FotoDiarioResposta(foto.getId(), foto.getChave(), eu.getId(), eu.getNome(), tripulacao.getNome(),
            foto.getLegenda(), foto.getCriadaEm());
    }

    // Quem anexou tira a própria foto do diário.
    @Transactional
    public void apagar(Long fotoId, Long congressistaId) {
        FotoDiario foto = buscarFoto(fotoId);
        if (!foto.getAutor().getId().equals(congressistaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só quem anexou pode tirar a foto do diário.");
        }
        fotos.delete(foto);
    }

    // Aba Diário do staff: as últimas fotos anexadas, de todas as tripulações.
    @Transactional(readOnly = true)
    public List<FotoDiarioResposta> recentes() {
        return fotos.recentes(PageRequest.of(0, LIMITE_STAFF));
    }

    // O staff tira qualquer foto do diário.
    @Transactional
    public void apagarPeloStaff(Long fotoId) {
        fotos.delete(buscarFoto(fotoId));
    }

    // Imagem pela chave do endereço. Vazio se a foto foi apagada.
    @Transactional(readOnly = true)
    public Optional<byte[]> lerFoto(String chave) {
        return fotos.conteudoPorChave(chave);
    }

    private Congressista buscarCongressista(Long id) {
        return congressistas.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
    }

    private static Tripulacao tripulacaoDe(Congressista pessoa) {
        if (pessoa.getTripulacao() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Você ainda não tem tripulação.");
        }
        return pessoa.getTripulacao();
    }

    private FotoDiario buscarFoto(Long id) {
        return fotos.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto não encontrada."));
    }
}

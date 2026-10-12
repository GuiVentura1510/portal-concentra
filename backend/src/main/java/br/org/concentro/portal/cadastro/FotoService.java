package br.org.concentro.portal.cadastro;

import br.org.concentro.portal.infra.Jpeg;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Foto de perfil: envio, troca, remoção e leitura.
// O site reduz a foto antes de enviar; aqui só se confere e se guarda.
@Service
public class FotoService {

    // O site envia fotos de uns 20 KB (no pior caso, 50 KB). O limite tem folga,
    // mas barra quem tentar mandar um arquivo grande direto para a API.
    static final int LIMITE_BYTES = 100 * 1024;

    // Quantas fotos ficam guardadas em memória (cada uma tem uns 20 KB).
    private static final int LIMITE_GUARDADAS = 1000;

    private final FotoPerfilRepository fotos;
    private final CongressistaRepository congressistas;

    // Fotos já lidas, pela chave. No evento, centenas de celulares pedem as mesmas
    // fotos (tripulação e ranking); com isto o banco só é consultado na primeira vez.
    // A chave muda a cada envio, então uma foto guardada nunca fica desatualizada.
    private final Map<String, byte[]> guardadas = new ConcurrentHashMap<>();

    public FotoService(FotoPerfilRepository fotos, CongressistaRepository congressistas) {
        this.fotos = fotos;
        this.congressistas = congressistas;
    }

    // Guarda a foto de quem está logado. Se a pessoa já tinha foto, troca.
    @Transactional
    public FotoResposta enviar(Long congressistaId, String imagemBase64) {
        byte[] conteudo = decodificar(imagemBase64);
        Congressista pessoa = congressistas.findById(congressistaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
        String chave = UUID.randomUUID().toString();
        Instant agora = Instant.now();

        FotoPerfil atual = fotos.findById(congressistaId).orElse(null);
        if (atual == null) {
            try {
                fotos.saveAndFlush(new FotoPerfil(congressistaId, chave, conteudo, agora));
            } catch (DataIntegrityViolationException e) {
                // Dois envios ao mesmo tempo da mesma pessoa: o banco barra o segundo.
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Sua foto já estava sendo enviada. Tente de novo.");
            }
        } else {
            // A chave antiga deixa de existir; a foto antiga sai da memória.
            guardadas.remove(atual.getChave());
            atual.trocar(chave, conteudo, agora);
        }
        pessoa.definirFoto(chave);
        return new FotoResposta(chave);
    }

    // A própria pessoa tira a foto.
    @Transactional
    public void remover(Long congressistaId) {
        Congressista pessoa = congressistas.findById(congressistaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conta não encontrada."));
        apagar(pessoa);
    }

    // O staff tira a foto de alguém (foto inadequada, por exemplo).
    @Transactional
    public void removerPeloStaff(Long congressistaId) {
        Congressista pessoa = congressistas.findById(congressistaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cadastro não encontrado."));
        apagar(pessoa);
    }

    // Apaga a foto da pessoa, se houver. Também é chamado antes de remover um cadastro,
    // para a foto sair da memória junto.
    @Transactional
    public void apagar(Congressista pessoa) {
        fotos.findById(pessoa.getId()).ifPresent(foto -> {
            guardadas.remove(foto.getChave());
            fotos.delete(foto);
        });
        pessoa.definirFoto(null);
    }

    // Imagem pela chave do endereço. Vazio se a chave não existe mais (foto trocada
    // ou removida). Sem transação de propósito: uma foto guardada em memória é
    // devolvida sem ocupar uma conexão do banco.
    public Optional<byte[]> ler(String chave) {
        byte[] guardada = guardadas.get(chave);
        if (guardada != null) {
            return Optional.of(guardada);
        }
        Optional<byte[]> lida = fotos.findByChave(chave).map(foto -> foto.getConteudo());
        lida.ifPresent(conteudo -> {
            if (guardadas.size() >= LIMITE_GUARDADAS) {
                guardadas.clear();
            }
            guardadas.put(chave, conteudo);
        });
        return lida;
    }

    // Confere o que chegou: base64 válido, dentro do limite e com cara de JPEG.
    private static byte[] decodificar(String imagemBase64) {
        return Jpeg.decodificar(imagemBase64, LIMITE_BYTES, "Foto grande demais.");
    }
}

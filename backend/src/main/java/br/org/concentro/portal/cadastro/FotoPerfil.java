package br.org.concentro.portal.cadastro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// Foto de perfil de um congressista. Espelha a tabela "foto_perfil" (V8).
// Fica separada do Congressista para as listas não carregarem os bytes da imagem.
@Entity
@Table(name = "foto_perfil")
public class FotoPerfil {

    // O id é o do próprio congressista: cada pessoa tem no máximo uma foto.
    @Id
    @Column(name = "congressista_id")
    private Long congressistaId;

    // Código sorteado a cada envio; é o que aparece no endereço da imagem.
    @Column(nullable = false, unique = true, length = 36)
    private String chave;

    // A imagem em si: um JPEG já reduzido pelo site.
    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;

    // Construtor vazio exigido pelo JPA.
    protected FotoPerfil() {
    }

    public FotoPerfil(Long congressistaId, String chave, byte[] conteudo, Instant agora) {
        this.congressistaId = congressistaId;
        this.chave = chave;
        this.conteudo = conteudo;
        this.enviadaEm = agora;
    }

    public Long getCongressistaId() {
        return congressistaId;
    }

    public String getChave() {
        return chave;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    // Foto nova da mesma pessoa: troca a imagem e a chave do endereço.
    public void trocar(String chave, byte[] conteudo, Instant agora) {
        this.chave = chave;
        this.conteudo = conteudo;
        this.enviadaEm = agora;
    }
}

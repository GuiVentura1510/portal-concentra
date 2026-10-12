package br.org.concentro.portal.missao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Foto de um envio de missão. Espelha a tabela "foto_envio" (V10).
// Fica separada da Conclusao para as listas não carregarem os bytes da imagem.
@Entity
@Table(name = "foto_envio")
public class FotoEnvio {

    // O id é o da própria conclusão: cada envio tem no máximo uma foto.
    @Id
    @Column(name = "conclusao_id")
    private Long conclusaoId;

    @Column(nullable = false, unique = true, length = 36)
    private String chave;

    // Um JPEG já reduzido pelo site.
    @Column(nullable = false)
    private byte[] conteudo;

    // Construtor vazio exigido pelo JPA.
    protected FotoEnvio() {
    }

    public FotoEnvio(Long conclusaoId, String chave, byte[] conteudo) {
        this.conclusaoId = conclusaoId;
        this.chave = chave;
        this.conteudo = conteudo;
    }

    public byte[] getConteudo() {
        return conteudo;
    }
}

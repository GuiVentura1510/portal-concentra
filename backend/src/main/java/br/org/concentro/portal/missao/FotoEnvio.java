package br.org.concentro.portal.missao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Foto de um envio de missão. Espelha a tabela "foto_envio" (V10 e V11).
// Fica separada da Conclusao para as listas não carregarem os bytes da imagem.
@Entity
@Table(name = "foto_envio")
public class FotoEnvio {

    // Código sorteado no envio, usado no endereço da imagem (V11: virou o id,
    // porque um envio pode ter até 5 fotos).
    @Id
    @Column(length = 36)
    private String chave;

    @Column(name = "conclusao_id", nullable = false)
    private Long conclusaoId;

    // Posição da foto no envio: 0, 1, 2...
    @Column(nullable = false)
    private int ordem;

    // Um JPEG já reduzido pelo site.
    @Column(nullable = false)
    private byte[] conteudo;

    // Construtor vazio exigido pelo JPA.
    protected FotoEnvio() {
    }

    public FotoEnvio(Long conclusaoId, int ordem, String chave, byte[] conteudo) {
        this.conclusaoId = conclusaoId;
        this.ordem = ordem;
        this.chave = chave;
        this.conteudo = conteudo;
    }

    public byte[] getConteudo() {
        return conteudo;
    }
}

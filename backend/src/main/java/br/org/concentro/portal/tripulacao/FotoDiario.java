package br.org.concentro.portal.tripulacao;

import br.org.concentro.portal.cadastro.Congressista;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

// Foto anexada ao Diário de Bordo de uma tripulação. Espelha a tabela "foto_diario" (V12).
// As listas usam consultas que não trazem os bytes da imagem (FotoDiarioRepository).
@Entity
@Table(name = "foto_diario")
public class FotoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripulacao_id", nullable = false)
    private Tripulacao tripulacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Congressista autor;

    // Código sorteado no envio; é o que aparece no endereço da imagem.
    @Column(nullable = false, unique = true, length = 36)
    private String chave;

    // Um JPEG já reduzido pelo site.
    @Column(nullable = false)
    private byte[] conteudo;

    // Escrita por quem anexou; vazia quando a pessoa não escreveu nada.
    @Column(length = 30)
    private String legenda;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    // Construtor vazio exigido pelo JPA.
    protected FotoDiario() {
    }

    public FotoDiario(Tripulacao tripulacao, Congressista autor, String chave, byte[] conteudo, String legenda,
            Instant agora) {
        this.tripulacao = tripulacao;
        this.autor = autor;
        this.chave = chave;
        this.conteudo = conteudo;
        this.legenda = legenda;
        this.criadaEm = agora;
    }

    public Long getId() {
        return id;
    }

    public Congressista getAutor() {
        return autor;
    }

    public String getChave() {
        return chave;
    }

    public String getLegenda() {
        return legenda;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}

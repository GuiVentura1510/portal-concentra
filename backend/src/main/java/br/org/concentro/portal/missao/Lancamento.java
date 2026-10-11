package br.org.concentro.portal.missao;

import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.tripulacao.Tripulacao;
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

// Uma linha do extrato de pontos de uma tripulação. Espelha a tabela "lancamento" (V6).
// Os pontos são copiados da missão no momento da aprovação: se o staff mudar os
// pontos da missão depois, o que já foi lançado não muda.
@Entity
@Table(name = "lancamento")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tripulacao_id")
    private Tripulacao tripulacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missao_id")
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conclusao_id")
    private Conclusao conclusao;

    @Column(nullable = false)
    private int pontos;

    @Column(nullable = false, length = 200)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por")
    private Congressista criadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    // Construtor vazio exigido pelo JPA.
    protected Lancamento() {
    }

    public Lancamento(Conclusao conclusao, Congressista criadoPor, Instant agora) {
        this.tripulacao = conclusao.getTripulacao();
        this.missao = conclusao.getMissao();
        this.conclusao = conclusao;
        this.pontos = conclusao.getMissao().getPontos();
        String titulo = conclusao.getMissao().getTitulo();
        this.descricao = titulo.length() > 190 ? titulo.substring(0, 190) : titulo;
        this.criadoPor = criadoPor;
        this.criadoEm = agora;
    }

    public Long getId() {
        return id;
    }

    public int getPontos() {
        return pontos;
    }
}

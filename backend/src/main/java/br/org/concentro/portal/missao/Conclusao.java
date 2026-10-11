package br.org.concentro.portal.missao;

import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.tripulacao.Tripulacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

// Uma tentativa de uma tripulação cumprir uma missão. Espelha a tabela "conclusao" (V6).
@Entity
@Table(name = "conclusao")
public class Conclusao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id")
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tripulacao_id")
    private Tripulacao tripulacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConclusao status;

    @Column(length = 4000)
    private String resposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviada_por")
    private Congressista enviadaPor;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avaliada_por")
    private Congressista avaliadaPor;

    @Column(name = "avaliada_em")
    private Instant avaliadaEm;

    @Column(name = "motivo_recusa", length = 500)
    private String motivoRecusa;

    // Construtor vazio exigido pelo JPA.
    protected Conclusao() {
    }

    // Resposta enviada pelo capitão; espera a avaliação do staff.
    public static Conclusao enviada(Missao missao, Tripulacao tripulacao, Congressista autor, String resposta,
            Instant agora) {
        Conclusao c = new Conclusao();
        c.missao = missao;
        c.tripulacao = tripulacao;
        c.status = StatusConclusao.PENDENTE;
        c.resposta = resposta;
        c.enviadaPor = autor;
        c.enviadaEm = agora;
        return c;
    }

    // Missão marcada como cumprida pelo próprio staff, já aprovada.
    public static Conclusao marcadaPeloStaff(Missao missao, Tripulacao tripulacao, Congressista staff,
            Instant agora) {
        Conclusao c = new Conclusao();
        c.missao = missao;
        c.tripulacao = tripulacao;
        c.status = StatusConclusao.APROVADA;
        c.enviadaPor = staff;
        c.enviadaEm = agora;
        c.avaliadaPor = staff;
        c.avaliadaEm = agora;
        return c;
    }

    public void aprovar(Congressista staff, Instant agora) {
        this.status = StatusConclusao.APROVADA;
        this.avaliadaPor = staff;
        this.avaliadaEm = agora;
    }

    public void recusar(Congressista staff, Instant agora, String motivo) {
        this.status = StatusConclusao.RECUSADA;
        this.avaliadaPor = staff;
        this.avaliadaEm = agora;
        this.motivoRecusa = motivo;
    }

    public Long getId() {
        return id;
    }

    public Missao getMissao() {
        return missao;
    }

    public Tripulacao getTripulacao() {
        return tripulacao;
    }

    public StatusConclusao getStatus() {
        return status;
    }

    public String getResposta() {
        return resposta;
    }

    public Congressista getEnviadaPor() {
        return enviadaPor;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    public String getMotivoRecusa() {
        return motivoRecusa;
    }
}

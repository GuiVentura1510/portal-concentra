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

// Uma tentativa de cumprir uma missão. Espelha a tabela "conclusao" (V6 e V7).
// Nas missões da tripulação, só a tripulação é preenchida. Nas individuais, a pessoa
// é preenchida e a tripulação dela também, se ela já tiver uma.
@Entity
@Table(name = "conclusao")
public class Conclusao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "missao_id")
    private Missao missao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripulacao_id")
    private Tripulacao tripulacao;

    // Quem cumpriu, nas missões individuais. Vazio nas missões da tripulação.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congressista_id")
    private Congressista congressista;

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

    // Chave da foto do envio (V10), cópia da tabela "foto_envio". Vazia sem foto.
    @Column(name = "foto_chave", length = 36)
    private String fotoChave;

    // Construtor vazio exigido pelo JPA.
    protected Conclusao() {
    }

    // Resposta enviada pelo capitão. Aceita na hora (decisão de 12/10): fica aprovada,
    // sem staff em "avaliada_por". O staff pode retirar depois.
    public static Conclusao enviada(Missao missao, Tripulacao tripulacao, Congressista autor, String resposta,
            Instant agora) {
        Conclusao c = new Conclusao();
        c.missao = missao;
        c.tripulacao = tripulacao;
        c.status = StatusConclusao.APROVADA;
        c.resposta = resposta;
        c.enviadaPor = autor;
        c.enviadaEm = agora;
        c.avaliadaEm = agora;
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

    // Resposta enviada pela própria pessoa numa missão individual. Aceita na hora,
    // como a da tripulação.
    public static Conclusao enviadaIndividual(Missao missao, Congressista pessoa, String resposta, Instant agora) {
        Conclusao c = new Conclusao();
        c.missao = missao;
        c.tripulacao = pessoa.getTripulacao();
        c.congressista = pessoa;
        c.status = StatusConclusao.APROVADA;
        c.resposta = resposta;
        c.enviadaPor = pessoa;
        c.enviadaEm = agora;
        c.avaliadaEm = agora;
        return c;
    }

    // Missão individual marcada como cumprida pelo staff, já aprovada.
    public static Conclusao marcadaParaPessoa(Missao missao, Congressista pessoa, Congressista staff,
            Instant agora) {
        Conclusao c = new Conclusao();
        c.missao = missao;
        c.tripulacao = pessoa.getTripulacao();
        c.congressista = pessoa;
        c.status = StatusConclusao.APROVADA;
        c.enviadaPor = staff;
        c.enviadaEm = agora;
        c.avaliadaPor = staff;
        c.avaliadaEm = agora;
        return c;
    }

    // A retirada pelo staff é feita no ConclusaoRepository.retirar, num "update ... where"
    // condicional, para dois staffs não retirarem o mesmo envio ao mesmo tempo.

    public Long getId() {
        return id;
    }

    public Missao getMissao() {
        return missao;
    }

    public Tripulacao getTripulacao() {
        return tripulacao;
    }

    public Congressista getCongressista() {
        return congressista;
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

    public String getFotoChave() {
        return fotoChave;
    }

    // Chamado antes de salvar o envio; a imagem é gravada logo depois, com o id.
    public void anexarFoto(String chave) {
        this.fotoChave = chave;
    }
}

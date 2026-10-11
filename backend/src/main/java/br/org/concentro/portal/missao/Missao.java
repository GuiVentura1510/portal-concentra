package br.org.concentro.portal.missao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

// Missão da expedição, criada pelo staff. Espelha a tabela "missao" (V5).
// Os horários ficam em UTC; o site mostra no horário de Brasília.
@Entity
@Table(name = "missao")
public class Missao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String descricao;

    @Column(nullable = false)
    private int pontos;

    // Vazio = sem limite. Conta tripulações ou pessoas, conforme o alcance.
    private Integer vagas;

    private Instant inicio;

    private Instant fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaConclusao conclusao;

    @Column(nullable = false)
    private boolean visivel;

    // Da tripulação ou individual (V7). Só muda enquanto a missão não tem respostas.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlcanceMissao alcance = AlcanceMissao.TRIPULACAO;

    // Vagas já ocupadas. Só muda pelo MissaoRepository.ocuparVaga (V6).
    @Column(name = "vagas_usadas", nullable = false)
    private int vagasUsadas;

    // Construtor vazio exigido pelo JPA.
    protected Missao() {
    }

    public Missao(String titulo, String descricao, int pontos, Integer vagas, Instant inicio, Instant fim,
            FormaConclusao conclusao, boolean visivel) {
        atualizar(titulo, descricao, pontos, vagas, inicio, fim, conclusao, visivel);
    }

    public Missao(String titulo, String descricao, int pontos, Integer vagas, Instant inicio, Instant fim,
            FormaConclusao conclusao, boolean visivel, AlcanceMissao alcance) {
        atualizar(titulo, descricao, pontos, vagas, inicio, fim, conclusao, visivel, alcance);
    }

    public void atualizar(String titulo, String descricao, int pontos, Integer vagas, Instant inicio,
            Instant fim, FormaConclusao conclusao, boolean visivel, AlcanceMissao alcance) {
        atualizar(titulo, descricao, pontos, vagas, inicio, fim, conclusao, visivel);
        this.alcance = alcance;
    }

    public void atualizar(String titulo, String descricao, int pontos, Integer vagas, Instant inicio,
            Instant fim, FormaConclusao conclusao, boolean visivel) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.pontos = pontos;
        this.vagas = vagas;
        this.inicio = inicio;
        this.fim = fim;
        this.conclusao = conclusao;
        this.visivel = visivel;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getPontos() {
        return pontos;
    }

    public Integer getVagas() {
        return vagas;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFim() {
        return fim;
    }

    public FormaConclusao getConclusao() {
        return conclusao;
    }

    public boolean isVisivel() {
        return visivel;
    }

    public AlcanceMissao getAlcance() {
        return alcance;
    }

    public boolean individual() {
        return alcance == AlcanceMissao.INDIVIDUAL;
    }

    public int getVagasUsadas() {
        return vagasUsadas;
    }

    public boolean esgotada() {
        return vagas != null && vagasUsadas >= vagas;
    }
}

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

// Grupo de 5 a 6 congressistas de EJs diferentes. Espelha a tabela "tripulacao" (V4).
@Entity
@Table(name = "tripulacao")
public class Tripulacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome de constelação, dado na montagem.
    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    // Onde a tripulação se encontra no evento. Preenchido pelo staff.
    @Column(name = "ponto_encontro", length = 200)
    private String pontoEncontro;

    // Escolhido pela tripulação no evento e registrado pelo staff.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capitao_id")
    private Congressista capitao;

    // Construtor vazio exigido pelo JPA.
    protected Tripulacao() {
    }

    public Tripulacao(String nome) {
        this.nome = nome;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getPontoEncontro() {
        return pontoEncontro;
    }

    public Congressista getCapitao() {
        return capitao;
    }

    public void definirCapitao(Congressista capitao) {
        this.capitao = capitao;
    }

    public void definirPontoEncontro(String pontoEncontro) {
        this.pontoEncontro = pontoEncontro;
    }
}

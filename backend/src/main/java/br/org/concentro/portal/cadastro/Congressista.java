package br.org.concentro.portal.cadastro;

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
import br.org.concentro.portal.tripulacao.Tripulacao;

// Pessoa cadastrada no portal. Espelha a tabela "congressista" (V2 e V3).
// Cargo, tempo de MEJ e área de atuação são respondidos no próprio cadastro.
@Entity
@Table(name = "congressista")
public class Congressista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    // Sempre em minúsculas: o banco recusa e-mail com letra maiúscula.
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    // Hash BCrypt da senha. A senha em si nunca é gravada.
    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ej_id", nullable = false)
    private Ej ej;

    // Respondido no cadastro, por isso obrigatório.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tempo_mej", length = 30)
    private TempoMej tempoMej;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_atuacao", length = 50)
    private AreaAtuacao areaAtuacao;

    // Vazia até a montagem das tripulações (V4).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tripulacao_id")
    private Tripulacao tripulacao;

    // Construtor vazio exigido pelo JPA.
    protected Congressista() {
    }

    public Congressista(String nome, String email, String senhaHash, Ej ej, Cargo cargo,
            TempoMej tempoMej, AreaAtuacao areaAtuacao) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.ej = ej;
        this.cargo = cargo;
        this.tempoMej = tempoMej;
        this.areaAtuacao = areaAtuacao;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Ej getEj() {
        return ej;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public TempoMej getTempoMej() {
        return tempoMej;
    }

    public AreaAtuacao getAreaAtuacao() {
        return areaAtuacao;
    }

    public Tripulacao getTripulacao() {
        return tripulacao;
    }

    public void entrarNa(Tripulacao tripulacao) {
        this.tripulacao = tripulacao;
    }

    // Usado pelo staff quando a pessoa esquece a senha.
    public void trocarSenha(String novoHash) {
        this.senhaHash = novoHash;
    }
}

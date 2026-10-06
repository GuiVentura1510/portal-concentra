package br.org.concentro.portal.cadastro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Empresa júnior participante do evento. Espelha a tabela "ej", que só
// recebe linhas por migração; por isso a classe não tem setters.
@Entity
@Table(name = "ej")
public class Ej {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    // Construtor vazio exigido pelo JPA para montar o objeto a partir do banco.
    protected Ej() {
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }
}

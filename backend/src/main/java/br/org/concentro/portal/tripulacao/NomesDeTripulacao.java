package br.org.concentro.portal.tripulacao;

import java.util.List;

// Nomes das tripulações: as 88 constelações oficiais, com as mais conhecidas
// primeiro. Se houver mais de 88 tripulações, os nomes recomeçam com número.
public final class NomesDeTripulacao {

    static final List<String> CONSTELACOES = List.of(
        "Órion", "Cruzeiro do Sul", "Andrômeda", "Pégaso", "Cassiopeia", "Fênix", "Dragão",
        "Lira", "Perseu", "Centauro", "Escorpião", "Sagitário", "Hércules", "Cisne", "Águia",
        "Ursa Maior", "Ursa Menor", "Leão", "Touro", "Gêmeos", "Áries", "Câncer", "Virgem",
        "Libra", "Capricórnio", "Aquário", "Peixes", "Cão Maior", "Cão Menor", "Lebre",
        "Pomba", "Vela", "Quilha", "Popa", "Bússola", "Compasso", "Telescópio", "Microscópio",
        "Sextante", "Oitante", "Relógio", "Retículo", "Triângulo", "Triângulo Austral",
        "Coroa Boreal", "Coroa Austral", "Cefeu", "Cocheiro", "Boieiro", "Ofiúco", "Serpente",
        "Hidra", "Hidra Macho", "Erídano", "Baleia", "Golfinho", "Dourado", "Peixe Voador",
        "Peixe Austral", "Tucano", "Pavão", "Grou", "Ave do Paraíso", "Camaleão", "Unicórnio",
        "Girafa", "Lince", "Raposa", "Lobo", "Lagarto", "Flecha", "Escudo", "Altar", "Taça",
        "Corvo", "Cabeleira de Berenice", "Cães de Caça", "Leão Menor", "Cavalo Menor",
        "Escultor", "Fornalha", "Buril", "Pintor", "Índio", "Máquina Pneumática", "Mesa",
        "Mosca", "Régua");

    private NomesDeTripulacao() {
    }

    // Nome da tripulação na posição informada (0 = primeira).
    public static String nome(int posicao) {
        String base = CONSTELACOES.get(posicao % CONSTELACOES.size());
        int rodada = posicao / CONSTELACOES.size();
        return rodada == 0 ? base : base + " " + (rodada + 1);
    }
}

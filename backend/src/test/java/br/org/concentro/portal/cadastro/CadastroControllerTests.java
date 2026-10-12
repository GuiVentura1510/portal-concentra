package br.org.concentro.portal.cadastro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// Cada teste roda dentro de uma transação desfeita no final,
// então nada fica gravado no banco local.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CadastroControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    private Long umaEj() {
        return ejs.findAllByOrderByNomeAsc().get(0).getId();
    }

    private ResultActions cadastrar(String nome, String email, String senha, Long ejId, String cargo)
            throws Exception {
        return cadastrar(nome, email, senha, ejId, cargo, "\"PROJETOS\"");
    }

    // area vai como JSON: "\"PROJETOS\"" ou "null".
    private ResultActions cadastrar(String nome, String email, String senha, Long ejId, String cargo, String area)
            throws Exception {
        String corpo = """
            {"nome": "%s", "email": "%s", "senha": "%s", "ejId": %d, "cargo": "%s", "tempoMej": "DE_1_A_2_ANOS", "areaAtuacao": %s}
            """.formatted(nome, email, senha, ejId, cargo, area);
        return mvc.perform(post("/api/cadastro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo));
    }

    @Test
    void cadastraSemLoginEGuardaSoOHashDaSenha() throws Exception {
        cadastrar("Maria Teste", "Maria.Teste@Exemplo.com", "senhaSegura123", umaEj(), "DIRETOR")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("maria.teste@exemplo.com"))
            .andExpect(jsonPath("$.senha").doesNotExist());

        Congressista salvo = congressistas.findByEmail("maria.teste@exemplo.com").orElseThrow();
        assertThat(salvo.getSenhaHash()).isNotEqualTo("senhaSegura123").startsWith("$2");
        assertThat(salvo.getCargo()).isEqualTo(Cargo.DIRETOR);
        assertThat(salvo.getTempoMej()).isEqualTo(TempoMej.DE_1_A_2_ANOS);
        assertThat(salvo.getAreaAtuacao()).isEqualTo(AreaAtuacao.PROJETOS);
    }

    @Test
    void recusaEmailRepetidoMesmoComOutraCaixa() throws Exception {
        cadastrar("Ana", "ana.teste@exemplo.com", "senhaSegura123", umaEj(), "PRESIDENTE")
            .andExpect(status().isCreated());
        cadastrar("Ana de novo", "ANA.TESTE@exemplo.com", "outraSenha456", umaEj(), "PRESIDENTE")
            .andExpect(status().isConflict());
    }

    @Test
    void recusaSenhaCurta() throws Exception {
        cadastrar("João", "joao.teste@exemplo.com", "123", umaEj(), "DIRETOR")
            .andExpect(status().isBadRequest());
    }

    @Test
    void recusaEmailInvalido() throws Exception {
        cadastrar("João", "isso-nao-e-email", "senhaSegura123", umaEj(), "DIRETOR")
            .andExpect(status().isBadRequest());
    }

    @Test
    void recusaEjInexistente() throws Exception {
        cadastrar("João", "joao2.teste@exemplo.com", "senhaSegura123", 999999L, "DIRETOR")
            .andExpect(status().isBadRequest());
    }

    @Test
    void recusaCargoForaDaLista() throws Exception {
        cadastrar("João", "joao3.teste@exemplo.com", "senhaSegura123", umaEj(), "ESTAGIARIO")
            .andExpect(status().isBadRequest());
    }

    @Test
    void traineeCadastraSemArea() throws Exception {
        cadastrar("Tiago Trainee", "tiago.trainee@exemplo.com", "senhaSegura123", umaEj(), "TRAINEE", "null")
            .andExpect(status().isCreated());

        Congressista salvo = congressistas.findByEmail("tiago.trainee@exemplo.com").orElseThrow();
        assertThat(salvo.getCargo()).isEqualTo(Cargo.TRAINEE);
        assertThat(salvo.getAreaAtuacao()).isNull();
    }

    @Test
    void traineeQueEnviaAreaFicaSemArea() throws Exception {
        cadastrar("Tina Trainee", "tina.trainee@exemplo.com", "senhaSegura123", umaEj(), "TRAINEE", "\"MARKETING\"")
            .andExpect(status().isCreated());

        assertThat(congressistas.findByEmail("tina.trainee@exemplo.com").orElseThrow().getAreaAtuacao()).isNull();
    }

    @Test
    void projetistaPrecisaDeArea() throws Exception {
        cadastrar("Paula", "paula.projetista@exemplo.com", "senhaSegura123", umaEj(), "PROJETISTA", "null")
            .andExpect(status().isBadRequest());
        cadastrar("Paula", "paula.projetista@exemplo.com", "senhaSegura123", umaEj(), "PROJETISTA", "\"COMERCIAL\"")
            .andExpect(status().isCreated());
    }
}

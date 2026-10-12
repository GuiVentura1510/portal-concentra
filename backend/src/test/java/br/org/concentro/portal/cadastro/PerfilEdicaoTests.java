package br.org.concentro.portal.cadastro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.org.concentro.portal.missao.ConclusaoRepository;
import br.org.concentro.portal.missao.LancamentoRepository;
import br.org.concentro.portal.tripulacao.Tripulacao;
import br.org.concentro.portal.tripulacao.TripulacaoRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Edição do próprio perfil (PUT /api/perfil) e troca de senha
// (PUT /api/perfil/senha), com a regra de EJ e tempo de MEJ travados depois
// da montagem das tripulações.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerfilEdicaoTests {

    private static final String SENHA = "senhaSegura123";
    private static final String EMAIL = "carla.perfil@exemplo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    @Autowired
    private TripulacaoRepository tripulacoes;

    @Autowired
    private ConclusaoRepository conclusoes;

    @Autowired
    private LancamentoRepository lancamentos;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    private List<Ej> duasEjs;

    // Começa sem tripulações (o banco local pode ter uma montagem de teste
    // manual); tudo é desfeito no fim do teste.
    @BeforeEach
    void criaConta() {
        lancamentos.deleteAll();
        conclusoes.deleteAll();
        for (Congressista c : congressistas.comTripulacao()) {
            c.entrarNa(null);
        }
        em.flush();
        tripulacoes.deleteAll();
        em.flush();

        duasEjs = ejs.findAllByOrderByNomeAsc().subList(0, 2);
        congressistas.save(new Congressista("Carla Perfil", EMAIL, passwordEncoder.encode(SENHA), duasEjs.get(0),
            Cargo.ASSESSOR_CONSULTOR, TempoMej.MENOS_DE_6_MESES, AreaAtuacao.MARKETING));
    }

    private String token(String senha) throws Exception {
        String resposta = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(EMAIL, senha)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private String dados(String nome, Ej ej, String cargo, String tempo, String area) {
        return """
            {"nome": "%s", "ejId": %d, "cargo": "%s", "tempoMej": "%s", "areaAtuacao": "%s"}
            """.formatted(nome, ej.getId(), cargo, tempo, area);
    }

    private void montaUmaTripulacao() {
        tripulacoes.saveAndFlush(new Tripulacao("Órion"));
    }

    @Test
    void mudaNomeCargoEAreaSemMexerNoEmail() throws Exception {
        mvc.perform(put("/api/perfil").header("Authorization", token(SENHA)).contentType(MediaType.APPLICATION_JSON)
                .content(dados("  Carla Souza  ", duasEjs.get(0), "DIRETOR", "MENOS_DE_6_MESES", "PROJETOS")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Carla Souza"))
            .andExpect(jsonPath("$.cargo").value("DIRETOR"))
            .andExpect(jsonPath("$.areaAtuacao").value("PROJETOS"))
            .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void mudaEjETempoAntesDaMontagem() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization", token(SENHA)))
            .andExpect(jsonPath("$.podeMudarEjETempo").value(true));

        mvc.perform(put("/api/perfil").header("Authorization", token(SENHA)).contentType(MediaType.APPLICATION_JSON)
                .content(dados("Carla Perfil", duasEjs.get(1), "ASSESSOR_CONSULTOR", "MAIS_DE_2_ANOS", "MARKETING")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ejId").value(duasEjs.get(1).getId()))
            .andExpect(jsonPath("$.tempoMej").value("MAIS_DE_2_ANOS"));
    }

    @Test
    void depoisDaMontagemEjETempoFicamTravados() throws Exception {
        montaUmaTripulacao();
        String token = token(SENHA);

        mvc.perform(get("/api/perfil").header("Authorization", token))
            .andExpect(jsonPath("$.podeMudarEjETempo").value(false));

        mvc.perform(put("/api/perfil").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(dados("Carla Perfil", duasEjs.get(1), "ASSESSOR_CONSULTOR", "MENOS_DE_6_MESES", "MARKETING")))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/perfil").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(dados("Carla Perfil", duasEjs.get(0), "ASSESSOR_CONSULTOR", "MAIS_DE_2_ANOS", "MARKETING")))
            .andExpect(status().isConflict());

        // Com EJ e tempo iguais, o resto continua editável.
        mvc.perform(put("/api/perfil").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content(dados("Carla Nova", duasEjs.get(0), "PRESIDENTE", "MENOS_DE_6_MESES", "COMERCIAL")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Carla Nova"));
    }

    @Test
    void recusaDadosInvalidos() throws Exception {
        mvc.perform(put("/api/perfil").header("Authorization", token(SENHA)).contentType(MediaType.APPLICATION_JSON)
                .content(dados("   ", duasEjs.get(0), "DIRETOR", "MENOS_DE_6_MESES", "PROJETOS")))
            .andExpect(status().isBadRequest());
    }

    @Test
    void exigeLogin() throws Exception {
        mvc.perform(put("/api/perfil").contentType(MediaType.APPLICATION_JSON)
                .content(dados("Carla", duasEjs.get(0), "DIRETOR", "MENOS_DE_6_MESES", "PROJETOS")))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/perfil/senha").contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\": \"x\", \"novaSenha\": \"novaSenha123\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void trocaSenhaComASenhaAtual() throws Exception {
        mvc.perform(put("/api/perfil/senha").header("Authorization", token(SENHA)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\": \"%s\", \"novaSenha\": \"outraSenha456\"}".formatted(SENHA)))
            .andExpect(status().isNoContent());

        token("outraSenha456");
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"senha\": \"%s\"}".formatted(EMAIL, SENHA)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recusaSenhaAtualErradaOuNovaCurta() throws Exception {
        String token = token(SENHA);
        mvc.perform(put("/api/perfil/senha").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\": \"errada123\", \"novaSenha\": \"outraSenha456\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/perfil/senha").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"senhaAtual\": \"%s\", \"novaSenha\": \"curta\"}".formatted(SENHA)))
            .andExpect(status().isBadRequest());
    }
}

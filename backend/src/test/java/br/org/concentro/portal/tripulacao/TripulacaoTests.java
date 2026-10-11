package br.org.concentro.portal.tripulacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.org.concentro.portal.cadastro.AreaAtuacao;
import br.org.concentro.portal.cadastro.Cargo;
import br.org.concentro.portal.cadastro.Congressista;
import br.org.concentro.portal.cadastro.CongressistaRepository;
import br.org.concentro.portal.cadastro.Ej;
import br.org.concentro.portal.cadastro.EjRepository;
import br.org.concentro.portal.cadastro.TempoMej;
import jakarta.persistence.EntityManager;
import com.jayway.jsonpath.JsonPath;
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

// Fluxo completo das tripulações pela API. A conta staff.teste@exemplo.com é do
// staff só neste teste. Tudo roda numa transação desfeita no final.
@SpringBootTest(properties = "app.staff.emails=staff.teste@exemplo.com")
@AutoConfigureMockMvc
@Transactional
class TripulacaoTests {

    private static final String SENHA = "senhaSegura123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EjRepository ejs;

    @Autowired
    private CongressistaRepository congressistas;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TripulacaoRepository tripulacoes;

    @Autowired
    private EntityManager em;

    // Começa sempre sem tripulações, mesmo que o banco local já tenha uma montagem
    // de teste manual. Tudo é desfeito no fim do teste.
    // Depois cria 12 congressistas em 4 EJs, mais uma conta do staff.
    @BeforeEach
    void criaContas() {
        for (Congressista c : congressistas.comTripulacao()) {
            c.entrarNa(null);
        }
        em.flush();
        tripulacoes.deleteAll();
        em.flush();

        String hash = passwordEncoder.encode(SENHA);
        List<Ej> quatroEjs = ejs.findAllByOrderByNomeAsc().subList(0, 4);
        for (int i = 0; i < 12; i++) {
            TempoMej tempo = i % 2 == 0 ? TempoMej.MENOS_DE_6_MESES : TempoMej.MAIS_DE_2_ANOS;
            congressistas.save(new Congressista("Pessoa " + i, "pessoa" + i + ".teste@exemplo.com", hash,
                quatroEjs.get(i % 4), Cargo.ASSESSOR_CONSULTOR, tempo, AreaAtuacao.PROJETOS));
        }
        congressistas.save(new Congressista("Staff Teste", "staff.teste@exemplo.com", hash,
            quatroEjs.get(0), Cargo.DIRETOR, TempoMej.MAIS_DE_2_ANOS, AreaAtuacao.PROJETOS));
    }

    private String token(String email) throws Exception {
        String corpo = """
            {"email": "%s", "senha": "%s"}
            """.formatted(email, SENHA);
        String resposta = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    private void montar() throws Exception {
        mvc.perform(post("/api/staff/tripulacoes/montar").header("Authorization", token("staff.teste@exemplo.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.colegasDeMesmaEj").isNumber());
    }

    @Test
    void antesDaMontagemATripulacaoAindaNaoExiste() throws Exception {
        mvc.perform(get("/api/tripulacao").header("Authorization", token("pessoa0.teste@exemplo.com")))
            .andExpect(status().isNotFound());
    }

    @Test
    void resumoContaCongressistasETripulacoes() throws Exception {
        String staff = token("staff.teste@exemplo.com");
        mvc.perform(get("/api/staff/tripulacoes/resumo").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tripulacoes").value(0))
            .andExpect(jsonPath("$.comTripulacao").value(0));
        montar();
        mvc.perform(get("/api/staff/tripulacoes/resumo").header("Authorization", staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tripulacoes").value(greaterThan(0)));
    }

    @Test
    void congressistaNaoPodeMontar() throws Exception {
        mvc.perform(post("/api/staff/tripulacoes/montar").header("Authorization", token("pessoa0.teste@exemplo.com")))
            .andExpect(status().isForbidden());
    }

    @Test
    void staffMontaECadaUmVeASuaTripulacao() throws Exception {
        montar();

        mvc.perform(get("/api/tripulacao").header("Authorization", token("pessoa0.teste@exemplo.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").isNotEmpty())
            .andExpect(jsonPath("$.membros[*].nome", hasItem("Pessoa 0")));

        // O staff não entra em tripulação.
        mvc.perform(get("/api/tripulacao").header("Authorization", token("staff.teste@exemplo.com")))
            .andExpect(status().isNotFound());
    }

    @Test
    void montarDuasVezesDa409() throws Exception {
        montar();
        mvc.perform(post("/api/staff/tripulacoes/montar").header("Authorization", token("staff.teste@exemplo.com")))
            .andExpect(status().isConflict());
    }

    @Test
    void quemSeCadastraDepoisJaEntraNumaTripulacao() throws Exception {
        montar();
        Long ejId = ejs.findAllByOrderByNomeAsc().get(5).getId();
        String cadastro = """
            {"nome": "Chegou Depois", "email": "depois.teste@exemplo.com", "senha": "%s", "ejId": %d,
             "cargo": "DIRETOR", "tempoMej": "DE_1_A_2_ANOS", "areaAtuacao": "MARKETING"}
            """.formatted(SENHA, ejId);
        mvc.perform(post("/api/cadastro").contentType(MediaType.APPLICATION_JSON).content(cadastro))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/tripulacao").header("Authorization", token("depois.teste@exemplo.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membros[*].nome", hasItem("Chegou Depois")));
    }

    @Test
    void capitaoPrecisaSerDaPropriaTripulacao() throws Exception {
        montar();
        String staff = token("staff.teste@exemplo.com");
        String minha = mvc.perform(get("/api/tripulacao").header("Authorization", token("pessoa0.teste@exemplo.com")))
            .andReturn().getResponse().getContentAsString();
        long tripulacaoId = ((Number) JsonPath.read(minha, "$.id")).longValue();
        long membroId = ((Number) JsonPath.read(minha, "$.membros[0].id")).longValue();
        long staffId = congressistas.findByEmail("staff.teste@exemplo.com").orElseThrow().getId();

        mvc.perform(put("/api/staff/tripulacoes/" + tripulacaoId + "/capitao").header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"congressistaId\": " + membroId + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.capitaoId").value(membroId));

        mvc.perform(put("/api/staff/tripulacoes/" + tripulacaoId + "/capitao").header("Authorization", staff)
                .contentType(MediaType.APPLICATION_JSON).content("{\"congressistaId\": " + staffId + "}"))
            .andExpect(status().isBadRequest());

        assertThat(staffId).isNotEqualTo(membroId);
    }

    @Test
    void staffDefineOPontoDeEncontro() throws Exception {
        montar();
        String minha = mvc.perform(get("/api/tripulacao").header("Authorization", token("pessoa1.teste@exemplo.com")))
            .andReturn().getResponse().getContentAsString();
        long tripulacaoId = ((Number) JsonPath.read(minha, "$.id")).longValue();

        mvc.perform(put("/api/staff/tripulacoes/" + tripulacaoId + "/ponto-encontro")
                .header("Authorization", token("staff.teste@exemplo.com"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"  Totem azul, hall de entrada  \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pontoEncontro").value("Totem azul, hall de entrada"));
    }
}
